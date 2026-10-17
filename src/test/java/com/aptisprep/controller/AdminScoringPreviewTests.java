package com.aptisprep.controller;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.entity.User;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.repository.CertificateRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminScoringPreviewTests {
    @Autowired MockMvc mvc;
    @Autowired CertificateRepository certificates;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired UserRepository users;
    @Autowired ScoreBandRepository bands;
    private Certificate certificate;
    private ScoringScheme scheme;

    @BeforeEach
    void seedRealScoringRows() {
        User admin = users.findByEmailIgnoreCase("admin@aptis.local").orElseGet(() -> {
            User value = new User(); value.setFullName("Test Admin"); value.setEmail("admin@aptis.local");
            value.setPasswordHash("test-hash"); value.setRole(com.aptisprep.enums.Role.ADMIN);
            value.setStatus(com.aptisprep.enums.UserStatus.ACTIVE); return users.save(value);
        });
        certificate = certificates.findByCodeIgnoreCase("TEST_CERT").orElseGet(() -> {
            Certificate value = new Certificate(); value.setName("Test certificate"); value.setCode("TEST_CERT"); value.setStatus(RecordStatus.ACTIVE); value.setCreatedBy(admin); return certificates.save(value);
        });
        scheme = schemes.findByCertificateIdOrderByVersionDesc(certificate.getId()).stream().findFirst().orElseGet(() -> {
            ScoringScheme value = new ScoringScheme(); value.setCertificate(certificate); value.setName("Test scheme"); value.setVersion("test-1"); value.setStatus(RecordStatus.ACTIVE); value.setEffectiveFrom(LocalDateTime.now()); return schemes.save(value);
        });
    }

    @Test void allScoringPagesRender() throws Exception {
        String certificateId = certificate.getId().toString(); String schemeId = scheme.getId().toString();
        for(String path : new String[]{"/admin/certificates","/admin/scoring-schemes","/admin/certificates/create","/admin/certificates/" + certificateId,"/admin/certificates/" + certificateId + "/edit","/admin/scoring-schemes/create","/admin/scoring-schemes/" + schemeId,"/admin/scoring-schemes/" + schemeId + "/edit"}) {
            mvc.perform(get(path).with(user("admin@aptis.local").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("api-key"))))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("spring.datasource"))));
        }
        mvc.perform(get("/admin/settings").with(user("admin@aptis.local").roles("ADMIN")))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/certificates"));
    }

    @Test void filtersAndRolesAreValidated() throws Exception {
        String schemeId = scheme.getId().toString();
        mvc.perform(get("/admin/scoring-schemes/" + schemeId).param("skill","Speaking").with(user("admin@aptis.local").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(model().attribute("selectedSkill","Speaking"));
        mvc.perform(get("/admin/scoring-schemes/" + schemeId).param("skill","Grammar & Vocabulary").with(user("admin@aptis.local").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Chưa có khoảng điểm")));
        for(String path : new String[]{"/admin/certificates","/admin/certificates/" + certificate.getId() + "/edit","/admin/scoring-schemes/" + schemeId}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("student@aptis.local").roles("STUDENT"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/certificates/9999").with(user("admin@aptis.local").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get("/admin/scoring-schemes/" + schemeId).param("skill","invalid").with(user("admin@aptis.local").roles("ADMIN"))).andExpect(status().isBadRequest());
    }

    @Test void adminWritesRequireCsrfAndRejectDuplicateOrOverlappingBands() throws Exception {
        mvc.perform(post("/admin/certificates")
                        .param("name", "New certificate").param("code", "NEW_CERT").param("status", "ACTIVE")
                        .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/certificates")
                        .param("name", "New certificate").param("code", "NEW_CERT").param("status", "ACTIVE")
                        .with(user("admin@aptis.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/certificates")
                        .param("name", "Duplicate certificate").param("code", "NEW_CERT").param("status", "ACTIVE")
                        .with(user("admin@aptis.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Mã chứng chỉ đã tồn tại")));

        mvc.perform(post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                        .param("skill", "Reading").param("cefrLevel", "A1").param("minScore", "0")
                        .param("maxScore", "9.99").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                        .param("skill", "Reading").param("cefrLevel", "A1").param("minScore", "0")
                        .param("maxScore", "9.99").with(user("admin@aptis.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        ScoreBand band = bands.findBySchemeIdAndSkillOrderByMinScoreAsc(scheme.getId(), "READING").get(0);

        mvc.perform(post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                        .param("skill", "Reading").param("cefrLevel", "A1")
                        .param("minScore", "0").param("maxScore", "9.99")
                        .with(user("admin@aptis.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("CEFR này đã có khoảng điểm")));
        mvc.perform(post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                        .param("skill", "Reading").param("id", band.getId().toString()).param("cefrLevel", "A1")
                        .param("minScore", "0").param("maxScore", "9.50")
                        .with(user("admin@aptis.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/scoring-schemes/" + scheme.getId() + "/bands")
                        .param("skill", "Reading").param("cefrLevel", "A2").param("minScore", "9.50")
                        .param("maxScore", "20").with(user("admin@aptis.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Khoảng điểm bị chồng lấn")));
    }
}
