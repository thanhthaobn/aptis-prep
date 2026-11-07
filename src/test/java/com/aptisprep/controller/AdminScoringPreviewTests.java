package com.aptisprep.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.entity.User;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.repository.CertificateRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import com.aptisprep.repository.UserRepository;
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
    @Autowired com.aptisprep.service.contract.ScoringPolicyContract scoringPolicy;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired UserRepository users;
    @Autowired ScoreBandRepository bands;
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
                                    value.setRole(com.aptisprep.enums.Role.ADMIN);
                                    value.setStatus(com.aptisprep.enums.UserStatus.ACTIVE);
                                    return users.save(value);
                                });
        certificate =
                certificates
                        .findByCodeIgnoreCase("TEST_CERT")
                        .orElseGet(
                                () -> {
                                    Certificate value = new Certificate();
                                    value.setName("Test certificate");
                                    value.setCode("TEST_CERT");
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
        var exam = new MockExamEntity();
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
    void savesCombinedSchemeWithSharedScaleAndIndividualRangesInOneRequest() throws Exception {
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
                                        .param("scale.rawMin", "0")
                                        .param("scale.rawMax", "50")
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
        var raw = new java.math.BigDecimal("33.25");
        org.junit.jupiter.api.Assertions.assertEquals(
                raw, scoringPolicy.scale(created.getId(), "READING", raw));
        mvc.perform(get(response.getRedirectedUrl()).with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("17.99")))
                .andExpect(content().string(containsString("12.99")));
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
        var threshold = new java.math.BigDecimal("25.00");
        org.junit.jupiter.api.Assertions.assertTrue(
                scoringPolicy.belowConfiguredTarget(created.getId(), "READING", below));
        org.junit.jupiter.api.Assertions.assertNull(
                scoringPolicy.estimatedLevel(created.getId(), "READING", below));
        org.junit.jupiter.api.Assertions.assertEquals(
                "B1", scoringPolicy.estimatedLevel(created.getId(), "READING", threshold));
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
