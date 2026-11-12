package com.aptisprep.mvc.controller;

import static com.aptisprep.mvc.security.SessionMvcRequests.csrf;
import static com.aptisprep.mvc.security.SessionMvcRequests.user;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.mvc.entity.Certificate;
import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.entity.ScoreBand;
import com.aptisprep.mvc.entity.ScoringScheme;
import com.aptisprep.mvc.entity.User;
import com.aptisprep.mvc.enums.RecordStatus;
import com.aptisprep.mvc.repository.CertificateRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.repository.ScoreBandRepository;
import com.aptisprep.mvc.repository.ScoringSchemeRepository;
import com.aptisprep.mvc.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class AdminScoringPreviewTests {
    @Autowired MockMvc mvc;
    @Autowired CertificateRepository certificates;
    @Autowired MockExamRepository exams;
    @Autowired com.aptisprep.mvc.service.scoring.ScoringPolicyService scoringPolicy;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired UserRepository users;
    @Autowired ScoreBandRepository bands;
    @Autowired com.aptisprep.mvc.repository.ScoringScaleRuleRepository scaleRules;
    private Certificate certificate;
    private ScoringScheme scheme;

    @BeforeEach
    void seedRealScoringRows() {
        User admin =
                users.findByEmailIgnoreCase("admin@aptis.local")
                        .orElseGet(
                                () -> {
                                    User value = new User();
                                    value.setFullName("Test Admin");
                                    value.setEmail("admin@aptis.local");
                                    value.setPasswordHash("test-hash");
                                    value.setRole(com.aptisprep.mvc.enums.Role.ADMIN);
                                    value.setStatus(com.aptisprep.mvc.enums.UserStatus.ACTIVE);
                                    return users.save(value);
                                });
        certificate =
                certificates
                        .findByCodeIgnoreCase("APTIS_GENERAL")
                        .orElseGet(
                                () -> {
                                    Certificate value = new Certificate();
                                    value.setName("Aptis ESOL General");
                                    value.setCode("APTIS_GENERAL");
                                    value.setStatus(RecordStatus.ACTIVE);
                                    value.setCreatedBy(admin);
                                    return certificates.save(value);
                                });
        scheme =
                schemes.findByCertificateIdOrderByVersionDesc(certificate.getId()).stream()
                        .findFirst()
                        .orElseGet(
                                () -> {
                                    ScoringScheme value = new ScoringScheme();
                                    value.setCertificate(certificate);
                                    value.setName("Test scheme");
                                    value.setVersion("test-1");
                                    value.setStatus(RecordStatus.ACTIVE);
                                    value.setEffectiveFrom(LocalDateTime.now());
                                    return schemes.save(value);
                                });
    }

    @Test
    @Transactional
    void separateListsPaginateAndSchemesFilterByAssignedExamType() throws Exception {
        for (int index = 1; index <= 13; index++) {
            Certificate extra = new Certificate();
            extra.setCode("PAGING_" + index);
            extra.setName("Pagination certificate " + index);
            extra.setStatus(RecordStatus.ACTIVE);
            extra.setCreatedBy(certificate.getCreatedBy());
            certificates.save(extra);

            ScoringScheme extraScheme = new ScoringScheme();
            extraScheme.setCertificate(certificate);
            extraScheme.setName("Pagination scheme " + index);
            extraScheme.setVersion("paging-" + index);
            extraScheme.setStatus(RecordStatus.ACTIVE);
            extraScheme.setEffectiveFrom(LocalDateTime.now());
            schemes.save(extraScheme);
        }

        mvc.perform(
                        get("/admin/certificates")
                                .param("certificatePage", "1")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(containsString("/admin/certificates?certificatePage=0")))
                .andExpect(content().string(not(containsString("Pagination scheme"))));
        var exam = new MockExam();
        exam.setTitle("Assigned full mock");
        exam.setCertificateId(certificate.getId());
        exam.setExamType("FULL_MOCK");
        exam.setScoringSchemeId(scheme.getId());
        exams.save(exam);
        mvc.perform(
                        get("/admin/scoring-schemes")
                                .param("examType", "FULL_MOCK")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(scheme.getName())))
                .andExpect(content().string(not(containsString("Pagination scheme"))));
        mvc.perform(
                        get("/admin/scoring-schemes")
                                .param("examType", "UNASSIGNED")
                                .param("certificateId", certificate.getId().toString())
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pagination scheme")))
                .andExpect(content().string(containsString("schemePage=1")))
                .andExpect(content().string(not(containsString(scheme.getName()))));
    }

    @Test
    @Transactional
    void aNewSchemeIsFilteredByItsScopeBeforeAnyExamUsesIt() throws Exception {
        mvc.perform(
                        post("/admin/scoring-schemes")
                                .param("certificateId", certificate.getId().toString())
                                .param("name", "Reading practice scope")
                                .param("version", "scope-test")
                                .param("examType", "PRACTICE")
                                .param("scopeSkill", "READING")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        var created =
                schemes.findByCertificateIdOrderByVersionDesc(certificate.getId()).stream()
                        .filter(value -> "scope-test".equals(value.getVersion()))
                        .findFirst()
                        .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("READING", created.getScopeSkill());
        mvc.perform(
                        post("/admin/exams")
                                .param("title", "Mismatched practice")
                                .param("examType", "PRACTICE")
                                .param("certificateId", certificate.getId().toString())
                                .param("scoringSchemeId", created.getId().toString())
                                .param("sections[0].skill", "LISTENING")
                                .param("sections[0].durationMinutes", "20")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bộ chấm điểm không phù hợp")));
        mvc.perform(
                        get("/admin/scoring-schemes")
                                .param("examType", "PRACTICE")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reading practice scope")));
        mvc.perform(
                        get("/admin/scoring-schemes")
                                .param("examType", "FULL_MOCK")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Reading practice scope"))));
    }

    @Test
    @Transactional
    void savesCombinedSchemeWithSkillSpecificScalesAndIndividualRangesInOneRequest()
            throws Exception {
        var admin = user("admin@aptis.local").roles("ADMIN");
        mvc.perform(get("/admin/scoring-schemes/create").with(admin))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(containsString("ranges[GRAMMAR_VOCABULARY_C2].maxScore")));
        var response =
                mvc.perform(
                                post("/admin/scoring-schemes")
                                        .with(admin)
                                        .with(csrf())
                                        .param("certificateId", certificate.getId().toString())
                                        .param("name", "Combined scoring editor")
                                        .param("version", "combined-editor")
                                        .param("examType", "FULL_MOCK")
                                        .param("configureScores", "true")
                                        .param("skillScales[READING].rawMin", "0")
                                        .param("skillScales[READING].rawMax", "40")
                                        .param("skillScales[READING].scaleMin", "0")
                                        .param("skillScales[READING].scaleMax", "50")
                                        .param("skillScales[LISTENING].rawMin", "0")
                                        .param("skillScales[LISTENING].rawMax", "50")
                                        .param("skillScales[LISTENING].scaleMin", "0")
                                        .param("skillScales[LISTENING].scaleMax", "50")
                                        .param("ranges[READING_A1].minScore", "0")
                                        .param("ranges[READING_A1].maxScore", "17.99")
                                        .param("ranges[LISTENING_A1].minScore", "0")
                                        .param("ranges[LISTENING_A1].maxScore", "12.99"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn()
                        .getResponse();
        var created =
                schemes.findByCertificateIdOrderByVersionDesc(certificate.getId()).stream()
                        .filter(value -> "combined-editor".equals(value.getVersion()))
                        .findFirst()
                        .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(
                2, bands.findBySchemeIdOrderBySkillAscMinScoreAsc(created.getId()).size());
        org.junit.jupiter.api.Assertions.assertNull(created.getEffectiveFrom());
        var raw = new java.math.BigDecimal("33.25");
        org.junit.jupiter.api.Assertions.assertEquals(
                new java.math.BigDecimal("41.56"),
                scoringPolicy.scale(created.getId(), "READING", raw));
        org.junit.jupiter.api.Assertions.assertEquals(
                raw, scoringPolicy.scale(created.getId(), "LISTENING", raw));
        mvc.perform(get(response.getRedirectedUrl()).with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("17.99")))
                .andExpect(content().string(containsString("12.99")));
    }

    @Test
    @Transactional
    void practicePreservesCustomScaleOnEditAndActivatesOnlyAfterSavingADraft() throws Exception {
        var admin = user("admin@aptis.local").roles("ADMIN");
        var request =
                post("/admin/scoring-schemes")
                        .with(admin)
                        .with(csrf())
                        .param("certificateId", certificate.getId().toString())
                        .param("name", "Practice CEFR A1-C2")
                        .param("version", "practice-six-levels")
                        .param("examType", "PRACTICE")
                        .param("scopeSkill", "READING")
                        .param("status", "ACTIVE")
                        .param("configureScores", "true")
                        .param("skillScales[READING].rawMin", "0")
                        .param("skillScales[READING].rawMax", "40")
                        .param("skillScales[READING].scaleMin", "0")
                        .param("skillScales[READING].scaleMax", "60");
        String[] levels = {"A1", "A2", "B1", "B2", "C1", "C2"};
        for (int i = 0; i < levels.length; i++) {
            request.param("ranges[READING_" + levels[i] + "].minScore", String.valueOf(i * 10))
                    .param(
                            "ranges[READING_" + levels[i] + "].maxScore",
                            i == 5
                                    ? "60"
                                    : java.math.BigDecimal.valueOf((i + 1) * 10)
                                            .subtract(new java.math.BigDecimal("0.01"))
                                            .toPlainString());
        }
        mvc.perform(request).andExpect(status().is3xxRedirection());
        var created =
                schemes.findByCertificateIdOrderByVersionDesc(certificate.getId()).stream()
                        .filter(value -> "practice-six-levels".equals(value.getVersion()))
                        .findFirst()
                        .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(RecordStatus.DRAFT, created.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                6, bands.findBySchemeIdOrderBySkillAscMinScoreAsc(created.getId()).size());

        // Reuse every submitted value while editing; the scale must stay 0–60, not become 0–40.
        var edit = post("/admin/scoring-schemes/" + created.getId()).with(admin).with(csrf());
        request.buildRequest(new org.springframework.mock.web.MockServletContext())
                .getParameterMap()
                .forEach(
                        (name, values) -> {
                            if (!"status".equals(name)) edit.param(name, values);
                        });
        edit.param("status", "DRAFT");
        mvc.perform(edit).andExpect(status().is3xxRedirection());
        var rule =
                scaleRules
                        .findBySchemeIdAndSkillIgnoreCase(created.getId(), "READING")
                        .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(
                new java.math.BigDecimal("40.00"), rule.getRawMax());
        org.junit.jupiter.api.Assertions.assertEquals(
                new java.math.BigDecimal("60.00"), rule.getScaleMax());
        mvc.perform(
                        post("/admin/scoring-schemes/{id}/activate", created.getId())
                                .with(admin)
                                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(
                RecordStatus.ACTIVE, schemes.findById(created.getId()).orElseThrow().getStatus());
    }

    @Test
    @Transactional
    void incompleteDraftCannotBeActivated() throws Exception {
        var draft = new ScoringScheme();
        draft.setCertificate(certificate);
        draft.setName("Incomplete draft");
        draft.setVersion("incomplete-draft");
        draft.setExamType("FULL_MOCK");
        draft.setStatus(RecordStatus.DRAFT);
        draft = schemes.saveAndFlush(draft);
        mvc.perform(
                        post("/admin/scoring-schemes/{id}/activate", draft.getId())
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", containsString("Chưa có thang điểm")));
        org.junit.jupiter.api.Assertions.assertEquals(RecordStatus.DRAFT, draft.getStatus());
    }

    @Test
    @Transactional
    void certificateCreationAlwaysSavesADraftBeforeExplicitActivation() throws Exception {
        var admin = user("admin@aptis.local").roles("ADMIN");
        mvc.perform(
                        post("/admin/certificates")
                                .with(admin)
                                .with(csrf())
                                .param("name", "Draft certificate")
                                .param("code", "DRAFT_CREATE_TEST")
                                .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection());
        var draft = certificates.findByCodeIgnoreCase("DRAFT_CREATE_TEST").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(RecordStatus.DRAFT, draft.getStatus());
        mvc.perform(
                        post("/admin/certificates/{id}/activate", draft.getId())
                                .with(admin)
                                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(RecordStatus.ACTIVE, draft.getStatus());
    }

    @Test
    @Transactional
    void singleSkillSchemeUsesOneLevelWithAnExplicitPassingScore() throws Exception {
        mvc.perform(
                        post("/admin/scoring-schemes")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf())
                                .param("certificateId", certificate.getId().toString())
                                .param("name", "Reading checkpoint target")
                                .param("version", "checkpoint-editor")
                                .param("examType", "CHECKPOINT")
                                .param("scopeSkill", "READING")
                                .param("configureScores", "true")
                                .param("status", "ACTIVE")
                                .param("effectiveFrom", "2099-01-01T09:00")
                                .param("scale.rawMin", "0")
                                .param("scale.rawMax", "50")
                                .param("targetLevel", "B1")
                                .param("passingScore", "25"))
                .andExpect(status().is3xxRedirection());
        var created =
                schemes.findByCertificateIdOrderByVersionDesc(certificate.getId()).stream()
                        .filter(value -> "checkpoint-editor".equals(value.getVersion()))
                        .findFirst()
                        .orElseThrow();
        var below = new java.math.BigDecimal("24.99");
        org.junit.jupiter.api.Assertions.assertNull(created.getEffectiveFrom());
        mvc.perform(
                        post("/admin/scoring-schemes/{id}/activate", created.getId())
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        var vietnamNow = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        org.junit.jupiter.api.Assertions.assertTrue(
                created.getEffectiveFrom().isAfter(vietnamNow.minusMinutes(1))
                        && !created.getEffectiveFrom().isAfter(vietnamNow));
        var threshold = new java.math.BigDecimal("25.00");
        org.junit.jupiter.api.Assertions.assertTrue(
                scoringPolicy.belowConfiguredTarget(created.getId(), "READING", below));
        org.junit.jupiter.api.Assertions.assertNull(
                scoringPolicy.estimatedLevel(created.getId(), "READING", below));
        org.junit.jupiter.api.Assertions.assertEquals(
                "B1", scoringPolicy.estimatedLevel(created.getId(), "READING", threshold));
    }

    @Test
    @Transactional
    void aptisGeneralRejectsC2ScoringBands() throws Exception {
        certificate.setCode("APTIS_GENERAL");
        certificates.saveAndFlush(certificate);
        mvc.perform(
                        post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf())
                                .param("skill", "READING")
                                .param("cefrLevel", "C2")
                                .param("minScore", "49")
                                .param("maxScore", "50"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(containsString("Aptis ESOL General chỉ có mức tối đa C1")));
    }

    @Test
    void allScoringPagesRender() throws Exception {
        String certificateId = certificate.getId().toString();
        String schemeId = scheme.getId().toString();
        for (String path :
                new String[] {
                    "/admin/certificates",
                    "/admin/scoring-schemes",
                    "/admin/certificates/create",
                    "/admin/certificates/" + certificateId,
                    "/admin/certificates/" + certificateId + "/edit",
                    "/admin/scoring-schemes/create",
                    "/admin/scoring-schemes/" + schemeId,
                    "/admin/scoring-schemes/" + schemeId + "/edit"
                }) {
            mvc.perform(get(path).with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("api-key"))))
                    .andExpect(content().string(containsString("name=\"_csrf\"")))
                    .andExpect(content().string(not(containsString("spring.datasource"))));
        }
        mvc.perform(get("/admin/settings").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/certificates"));
    }

    @Test
    void filtersAndRolesAreValidated() throws Exception {
        String schemeId = scheme.getId().toString();
        mvc.perform(
                        get("/admin/scoring-schemes/" + schemeId)
                                .param("skill", "Speaking")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedSkill", "Speaking"));
        mvc.perform(
                        get("/admin/scoring-schemes/" + schemeId)
                                .param("skill", "Grammar & Vocabulary")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chưa có khoảng điểm")));
        for (String path :
                new String[] {
                    "/admin/certificates",
                    "/admin/certificates/" + certificate.getId() + "/edit",
                    "/admin/scoring-schemes/" + schemeId
                }) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("student@aptis.local").roles("STUDENT")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/certificates/9999").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        get("/admin/scoring-schemes/" + schemeId)
                                .param("skill", "invalid")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminWritesRequireCsrfAndRejectDuplicateOrOverlappingBands() throws Exception {
        mvc.perform(
                        post("/admin/certificates")
                                .param("name", "New certificate")
                                .param("code", "NEW_CERT")
                                .param("status", "ACTIVE")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/admin/certificates")
                                .param("name", "New certificate")
                                .param("code", "NEW_CERT")
                                .param("status", "ACTIVE")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(
                        post("/admin/certificates")
                                .param("name", "Duplicate certificate")
                                .param("code", "NEW_CERT")
                                .param("status", "ACTIVE")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mã chứng chỉ đã tồn tại")));

        mvc.perform(
                        post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                                .param("skill", "Reading")
                                .param("cefrLevel", "A1")
                                .param("minScore", "0")
                                .param("maxScore", "9.99")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                                .param("skill", "Reading")
                                .param("cefrLevel", "A1")
                                .param("minScore", "0")
                                .param("maxScore", "9.99")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        ScoreBand band =
                bands.findBySchemeIdAndSkillOrderByMinScoreAsc(scheme.getId(), "READING").get(0);
        String editUrl = "/admin/scoring-schemes/" + scheme.getId() + "/edit";
        var admin = user("admin@aptis.local").roles("ADMIN");
        mvc.perform(get(editUrl).with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("addBand=true#band-editor")))
                .andExpect(content().string(not(containsString("id=\"band-editor\""))));
        mvc.perform(get(editUrl).param("addBand", "true").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"band-editor\"")));
        mvc.perform(get(editUrl).param("bandId", band.getId().toString()).with(admin))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        matchesPattern(
                                                "(?s).*id=\"bandMin\"[^>]*value=\"0\\.00\".*")))
                .andExpect(
                        content()
                                .string(
                                        matchesPattern(
                                                "(?s).*id=\"bandMax\"[^>]*value=\"9\\.99\".*")));

        mvc.perform(
                        post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                                .param("skill", "Reading")
                                .param("cefrLevel", "A1")
                                .param("minScore", "0")
                                .param("maxScore", "9.99")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CEFR này đã có khoảng điểm")));
        mvc.perform(
                        post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                                .param("skill", "Reading")
                                .param("id", band.getId().toString())
                                .param("cefrLevel", "A1")
                                .param("minScore", "0")
                                .param("maxScore", "9.50")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(
                        post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                                .param("skill", "Reading")
                                .param("cefrLevel", "A2")
                                .param("minScore", "9.50")
                                .param("maxScore", "20")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Khoảng điểm bị chồng lấn")));
    }
}
