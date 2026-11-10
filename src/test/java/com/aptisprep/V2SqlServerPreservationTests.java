package com.aptisprep;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.exam.entity.*;
import com.aptisprep.mvc.learning.dto.*;
import com.aptisprep.mvc.learning.dto.LessonFormDTO;
import com.aptisprep.mvc.learning.entity.*;
import com.aptisprep.mvc.learning.repository.*;
import com.aptisprep.mvc.learning.service.*;
import com.aptisprep.mvc.learning.service.impl.*;
import com.aptisprep.repository.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Installs current DDL in an isolated database; never changes the application database. */
@EnabledIfEnvironmentVariable(named = "APTIS_SQLSERVER_ISOLATED_TESTS", matches = "true")
class V2SqlServerPreservationTests {
    @Test
    void currentSchemaCreatesAnEmptyDatabaseAndRefusesToOverwriteExistingData() throws Exception {
        String configured =
                System.getenv()
                        .getOrDefault(
                                "DB_URL",
                                "jdbc:sqlserver://localhost:1433;databaseName=aptisprep_db;encrypt=true;trustServerCertificate=true");
        String server = configured.replaceAll("(?i);databaseName=[^;]*", "");
        String login = System.getenv().getOrDefault("DB_USERNAME", "sa");
        String password = System.getenv("DB_PASSWORD");
        assertNotNull(password, "Set DB_PASSWORD for isolated SQL Server tests");
        String name = "codex_v2_test_" + UUID.randomUUID().toString().replace("-", "");
        assertTrue(name.matches("codex_v2_test_[a-f0-9]{32}"));
        try (var master =
                DriverManager.getConnection(server + ";databaseName=master", login, password)) {
            master.createStatement().execute("CREATE DATABASE [" + name + "]");
            try {
                String url = server + ";databaseName=" + name;
                try (var connection = DriverManager.getConnection(url, login, password);
                        var sql = connection.createStatement()) {
                    installCurrentSchema(connection);
                    verifyDocumentedSchema(connection);
                    try (var tables =
                            sql.executeQuery(
                                    "SELECT COUNT(*) FROM sys.tables WHERE is_ms_shipped = 0")) {
                        assertTrue(tables.next());
                        assertEquals(39, tables.getInt(1));
                    }
                    try (var rows = sql.executeQuery("SELECT COUNT(*) FROM users")) {
                        assertTrue(rows.next());
                        assertEquals(0, rows.getInt(1), "No demo accounts");
                    }
                    try (var rows = sql.executeQuery("SELECT COUNT(*) FROM mock_exams")) {
                        assertTrue(rows.next());
                        assertEquals(0, rows.getInt(1), "No demo exams");
                    }
                    int user =
                            insert(
                                    sql,
                                    "INSERT INTO users(full_name,email,password_hash,role,status)"
                                            + " OUTPUT INSERTED.id VALUES(N'Preserved"
                                            + " learner','preserved@test.local','unused','STUDENT','ACTIVE')");
                    int certificate =
                            insert(
                                    sql,
                                    "INSERT INTO certificates(name,code,status) OUTPUT INSERTED.id"
                                            + " VALUES(N'Test certificate','SQL-TEST','ACTIVE')");
                    int lesson =
                            insert(
                                    sql,
                                    "INSERT INTO"
                                            + " lessons(title,skill,cefr_level,display_order,status)"
                                            + " OUTPUT INSERTED.id VALUES(N'Preserved"
                                            + " lesson','Reading','A1',1,'PUBLISHED')");
                    int exam =
                            insert(
                                    sql,
                                    "INSERT INTO mock_exams(certificate_id,title,exam_type,status)"
                                            + " OUTPUT INSERTED.id VALUES("
                                            + certificate
                                            + ",N'Preserved exam','PRACTICE','DRAFT')");
                    int question =
                            insert(
                                    sql,
                                    "INSERT INTO questions(content,question_type,skill,status)"
                                            + " OUTPUT INSERTED.id VALUES(N'Preserved"
                                            + " writing','WRITING_TEXT','WRITING','DRAFT')");
                    int attempt =
                            insert(
                                    sql,
                                    "INSERT INTO"
                                            + " exam_attempts(user_id,mock_exam_id,status,started_at)"
                                            + " OUTPUT INSERTED.id VALUES("
                                            + user
                                            + ","
                                            + exam
                                            + ",'IN_PROGRESS',SYSDATETIME())");
                    sql.executeUpdate(
                            "INSERT INTO"
                                    + " user_answers(exam_attempt_id,question_id,status,text_answer)"
                                    + " VALUES("
                                    + attempt
                                    + ","
                                    + question
                                    + ",'ANSWERED',N'Original response')");
                    sql.executeUpdate(
                            "INSERT INTO"
                                    + " lesson_engagement(user_id,lesson_id,playback_sequence,playback_ranges_json)"
                                    + " VALUES("
                                    + user
                                    + ","
                                    + lesson
                                    + ",7,'[[0,45]]')");
                    for (int correct = 0; correct <= 5; correct++)
                        assertEquals(1, sql.executeUpdate(sixOptionCheck(correct)));
                    assertThrows(SQLException.class, () -> sql.executeUpdate(sixOptionCheck(6)));
                    assertThrows(SQLException.class, () -> sql.executeUpdate(sixOptionCheck(-1)));
                    Map<String, Snapshot> before = snapshot(connection);
                    var refused =
                            assertThrows(
                                    SQLException.class, () -> installCurrentSchema(connection));
                    assertEquals(50001, refused.getErrorCode());
                    for (var entry : before.entrySet()) {
                        assertEquals(
                                entry.getValue().json(),
                                read(connection, entry.getKey(), entry.getValue().columns()).json(),
                                "Reinstallation changed existing rows: " + entry.getKey());
                    }
                    // Filtered uniqueness must allow multiple legacy NULL request keys.
                    sql.executeUpdate(
                            "INSERT INTO exam_attempts(user_id,mock_exam_id,status,started_at)"
                                    + " VALUES("
                                    + user
                                    + ","
                                    + exam
                                    + ",'IN_PROGRESS',SYSDATETIME())");
                    try (var rows =
                            sql.executeQuery(
                                    "SELECT filter_definition FROM sys.indexes"
                                            + " WHERE name='uq_exam_attempts_user_request_key'")) {
                        assertTrue(rows.next());
                        assertTrue(rows.getString(1).contains("IS NOT NULL"));
                    }
                }
                validateEntityMappings(url, login, password);
                verifyKnowledgeCheckService(url, login, password);
            } finally {
                // Only the validated UUID database created above is ever removed.
                master.createStatement()
                        .execute(
                                "ALTER DATABASE ["
                                        + name
                                        + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                master.createStatement().execute("DROP DATABASE [" + name + "]");
            }
        }
    }

    private static void installCurrentSchema(Connection connection) throws Exception {
        String ddl = Files.readString(Path.of("database/schema-current.sql"));
        for (String batch : ddl.split("(?im)^\\s*GO\\s*\\r?$")) {
            if (!batch.isBlank())
                try (var sql = connection.createStatement()) {
                    sql.execute(batch);
                }
        }
    }

    private static void verifyDocumentedSchema(Connection connection) throws Exception {
        String markdown = Files.readString(Path.of("docs/BUSINESS_SPEC_CURRENT.md"));
        var diagrams =
                java.util.regex.Pattern.compile(
                                "```mermaid\\s+(.*?)```", java.util.regex.Pattern.DOTALL)
                        .matcher(markdown);
        var documented = new LinkedHashMap<String, Set<String>>();
        while (diagrams.find()) {
            var entities =
                    java.util.regex.Pattern.compile(
                                    "\\w+\\[\"([^\"]+)\"\\]\\s*\\{(.*?)\\}",
                                    java.util.regex.Pattern.DOTALL)
                            .matcher(diagrams.group(1));
            while (entities.find()) {
                var columns = new LinkedHashSet<String>();
                for (String line : entities.group(2).strip().split("\\R")) {
                    columns.add(line.strip().split("\\s+")[1]);
                }
                documented.put(entities.group(1), columns);
            }
        }
        var actual = new LinkedHashMap<String, Set<String>>();
        try (var tables =
                connection.getMetaData().getTables(null, "dbo", "%", new String[] {"TABLE"})) {
            while (tables.next()) {
                String table = tables.getString("TABLE_NAME");
                if (table.equals("flyway_schema_history")) continue;
                var columns = new LinkedHashSet<String>();
                try (var fields = connection.getMetaData().getColumns(null, "dbo", table, "%")) {
                    while (fields.next()) columns.add(fields.getString("COLUMN_NAME"));
                }
                actual.put(table, columns);
            }
        }
        assertEquals(
                actual,
                documented,
                "Mermaid must list every physical table and column through V041");
        assertEquals(39, documented.size());
        assertEquals(416, documented.values().stream().mapToInt(Set::size).sum());
    }

    /** Real repositories and service operations against the isolated SQL Server, not H2. */
    private static void verifyKnowledgeCheckService(String url, String login, String password) {
        var factory = new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(
                new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        url, login, password));
        factory.setPackagesToScan("com.aptisprep.entity");
        factory.setJpaVendorAdapter(
                new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
        try {
            factory.afterPropertiesSet();
            try (var em = Objects.requireNonNull(factory.getObject()).createEntityManager()) {
                em.getTransaction().begin();
                try {
                    var repositories =
                            new org.springframework.data.jpa.repository.support
                                    .JpaRepositoryFactory(em);
                    var checks = repositories.getRepository(LessonCheckRepository.class);
                    var lessons = repositories.getRepository(LessonRepository.class);
                    var users = repositories.getRepository(UserRepository.class);
                    var attempts = repositories.getRepository(LessonCheckAttemptRepository.class);
                    var teaching =
                            new LessonTeachingServiceImpl(
                                    new LessonCheckService(checks, lessons, users, attempts),
                                    lessons,
                                    users,
                                    repositories.getRepository(LessonResponseRepository.class),
                                    attempts,
                                    repositories.getRepository(LessonEngagementRepository.class),
                                    null,
                                    repositories.getRepository(LessonProgressRepository.class),
                                    null,
                                    new com.aptisprep.mvc.common.config.AiProperties());
                    // Storage and AI are intentionally unused by Knowledge Check operations.
                    var lesson = new Lesson();
                    lesson.setTitle("SQL Server Knowledge Check regression");
                    lesson.setSkill("Reading");
                    lesson.setCefrLevel("A1");
                    lesson.setDisplayOrder(
                            em.createQuery(
                                            "select coalesce(max(l.displayOrder), 0) + 1 from"
                                                    + " Lesson l where l.skill = :skill and l.cefrLevel"
                                                    + " = :level",
                                            Integer.class)
                                    .setParameter("skill", "Reading")
                                    .setParameter("level", "A1")
                                    .getSingleResult());
                    lesson.setStatus("PUBLISHED");
                    em.persist(lesson);
                    em.flush();
                    String email = "preserved@test.local";
                    for (int count : List.of(4, 6)) {
                        var form = checkForm(count, count - 1);
                        teaching.validate(form);
                        teaching.saveChecks(lesson.getId(), form.getChecks());
                        em.flush();
                        em.clear();
                        var question = teaching.questions(lesson.getId()).getFirst();
                        assertEquals(count, question.options().size());
                        String key = "q" + question.id();
                        var correct =
                                teaching.grade(
                                        lesson.getId(), Map.of(key, "" + (count - 1)), email);
                        assertEquals(1, correct.score());
                        assertEquals(1, correct.total());
                        assertTrue(correct.answers().getFirst().right());
                        assertEquals(
                                count == 4 ? "D" : "F", correct.answers().getFirst().correct());
                        var wrong = teaching.grade(lesson.getId(), Map.of(key, "0"), email);
                        assertEquals(0, wrong.score());
                        assertFalse(wrong.answers().getFirst().right());
                        em.flush();
                        em.clear();
                        var saved =
                                attempts.findByUserIdAndLessonIdOrderBySubmittedAtDesc(
                                        users.findByEmailIgnoreCase(email).orElseThrow().getId(),
                                        lesson.getId());
                        assertEquals(count == 4 ? 2 : 4, saved.size());
                        assertEquals(
                                count == 4 ? 1 : 2,
                                saved.stream().filter(attempt -> attempt.getScore() == 1).count());
                        assertEquals(
                                count == 4 ? 1 : 2,
                                saved.stream().filter(attempt -> attempt.getScore() == 0).count());
                        assertThrows(
                                BusinessRuleException.class,
                                () -> teaching.grade(lesson.getId(), Map.of(key, "-1"), email));
                        assertThrows(
                                BusinessRuleException.class,
                                () ->
                                        teaching.grade(
                                                lesson.getId(), Map.of(key, "" + count), email));
                        assertThrows(
                                BusinessRuleException.class,
                                () -> teaching.grade(lesson.getId(), Map.of(), email));
                    }
                    for (int invalid : List.of(-1, 4, 6)) {
                        assertThrows(
                                BusinessRuleException.class,
                                () -> teaching.validate(checkForm(4, invalid)));
                    }
                    var gap = checkForm(6, 5);
                    gap.getChecks().getFirst().setOptionC(" ");
                    assertThrows(BusinessRuleException.class, () -> teaching.validate(gap));
                    // Invalid requests did not overwrite the valid six-choice question or create
                    // attempts.
                    assertEquals(6, teaching.questions(lesson.getId()).getFirst().options().size());
                    assertEquals(4, attempts.findByLessonId(lesson.getId()).size());
                    verifyAiRecoveryQueries(
                            em,
                            repositories,
                            users.findByEmailIgnoreCase(email).orElseThrow().getId());
                    em.getTransaction().commit();
                } finally {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                }
            }
        } finally {
            if (factory.getObject() != null) factory.destroy();
        }
    }

    private static void verifyAiRecoveryQueries(
            jakarta.persistence.EntityManager em,
            org.springframework.data.jpa.repository.support.JpaRepositoryFactory repositories,
            int owner) {
        var jobs = repositories.getRepository(AiAssessmentJobRepository.class);
        var answer =
                em.createQuery(
                                "from UserAnswerEntity",
                                com.aptisprep.mvc.exam.entity.UserAnswerEntity.class)
                        .getResultList()
                        .getFirst();
        var examJob = sqlJob("sql-recovery-exam", owner, answer, "EXAM_ANSWER");
        var lessonJob = sqlJob("sql-recovery-lesson", owner, answer, "LESSON_SUBMISSION");
        em.persist(examJob);
        em.persist(lessonJob);
        em.flush();
        assertEquals(
                List.of(examJob.getAssessmentKey()),
                jobs.findExamJobs(owner, answer.getAttemptId(), 1, "p1", "r1").stream()
                        .map(com.aptisprep.entity.AiAssessmentJob::getAssessmentKey)
                        .toList());
        assertTrue(
                jobs.findRecoverableKeys(
                                com.aptisprep.mvc.common.enums.AiJobState.PENDING,
                                com.aptisprep.mvc.common.enums.AiJobState.PROCESSING,
                                java.time.LocalDateTime.now(),
                                java.time.LocalDateTime.now().minusMinutes(15),
                                org.springframework.data.domain.PageRequest.of(0, 50))
                        .contains(examJob.getAssessmentKey()));
        examJob.setState(com.aptisprep.mvc.common.enums.AiJobState.ERROR);
        lessonJob.setState(com.aptisprep.mvc.common.enums.AiJobState.ERROR);
        em.flush();
        assertEquals(
                List.of(examJob.getAssessmentKey()),
                jobs
                        .findExamJobsByState(
                                owner,
                                answer.getAttemptId(),
                                1,
                                "p1",
                                "r1",
                                com.aptisprep.mvc.common.enums.AiJobState.ERROR)
                        .stream()
                        .map(com.aptisprep.entity.AiAssessmentJob::getAssessmentKey)
                        .toList());
        examJob.setState(com.aptisprep.mvc.common.enums.AiJobState.COMPLETED);
        var assessment = new com.aptisprep.entity.AiAssessment();
        assessment.setJob(examJob);
        assessment.setAssessmentKey(examJob.getAssessmentKey());
        assessment.setAnswerId(answer.getId());
        assessment.setAnswerVersion(1);
        assessment.setPolicyVersion("p1");
        assessment.setRubricVersion("r1");
        assessment.setState(com.aptisprep.mvc.common.enums.AiAssessmentState.COMPLETED);
        assessment.setScore(java.math.BigDecimal.valueOf(20));
        em.persist(assessment);
        em.flush();
        assertTrue(
                jobs.findCompletedWithoutCallback(
                                org.springframework.data.domain.PageRequest.of(0, 50))
                        .contains(examJob.getAssessmentKey()));
        var row = new com.aptisprep.mvc.exam.entity.AttemptOutboxEntity();
        row.setEventKey("ai:sql-recovery-exam:v1:COMPLETED");
        row.setEventType("AI_ASSESSMENT_COMPLETED");
        row.setAggregateId(answer.getAttemptId());
        row.setPayload("{}");
        row.setStatus("DISPATCHED");
        em.persist(row);
        em.flush();
        assertFalse(
                jobs.findCompletedWithoutCallback(
                                org.springframework.data.domain.PageRequest.of(0, 50))
                        .contains(examJob.getAssessmentKey()));
    }

    private static com.aptisprep.entity.AiAssessmentJob sqlJob(
            String key,
            int owner,
            com.aptisprep.mvc.exam.entity.UserAnswerEntity answer,
            String source) {
        var job = new com.aptisprep.entity.AiAssessmentJob();
        job.setAssessmentKey(key);
        job.setUserId(owner);
        job.setAttemptId(answer.getAttemptId());
        job.setAnswerId(answer.getId());
        job.setSourceType(source);
        job.setSourceId(answer.getId());
        job.setAnswerVersion(1);
        job.setPolicyVersion("p1");
        job.setRubricVersion("r1");
        job.setSkill("WRITING");
        job.setTaskType("WRITING");
        job.setTaskContent("SQL regression task");
        job.setLockedContent("Preserved response");
        job.setMaxScore(java.math.BigDecimal.valueOf(50));
        job.setState(com.aptisprep.mvc.common.enums.AiJobState.PENDING);
        return job;
    }

    private static LessonFormDTO checkForm(int count, int correct) {
        var form = new LessonFormDTO();
        form.setSkill("Reading");
        form.setStatus("PUBLISHED");
        var check = new LessonFormDTO.CheckForm();
        check.setPrompt("Choose the last option");
        check.setOptionA("A");
        check.setOptionB("B");
        check.setOptionC("C");
        check.setOptionD("D");
        if (count == 6) {
            check.setOptionE("E");
            check.setOptionF("F");
        }
        check.setCorrectOption(correct);
        check.setExplanation("The final option is correct");
        form.getChecks().add(check);
        return form;
    }

    private static int insert(Statement sql, String query) throws SQLException {
        try (var rows = sql.executeQuery(query)) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }

    private static String sixOptionCheck(int correct) {
        return "INSERT INTO"
                + " lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,option_d,option_e,option_f,correct_option,explanation)"
                + " SELECT TOP 1 id,"
                + (9000 + correct)
                + ",N'Six options',N'A',N'B',N'C',N'D',N'E',N'F',"
                + correct
                + ",N'Explanation' FROM lessons ORDER BY id";
    }

    static void validateEntityMappings(String url, String login, String password) {
        var factory = new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(
                new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        url, login, password));
        factory.setPackagesToScan("com.aptisprep.entity");
        factory.setJpaVendorAdapter(
                new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
        try {
            factory.afterPropertiesSet();
            assertNotNull(factory.getObject());
        } finally {
            if (factory.getObject() != null) factory.destroy();
        }
    }

    private record Snapshot(List<String> columns, String json) {}

    private static Map<String, Snapshot> snapshot(Connection connection) throws SQLException {
        var result = new LinkedHashMap<String, Snapshot>();
        var names = new ArrayList<String>();
        try (var tables =
                connection.getMetaData().getTables(null, "dbo", "%", new String[] {"TABLE"})) {
            while (tables.next())
                if (!tables.getString("TABLE_NAME").equals("flyway_schema_history"))
                    names.add(tables.getString("TABLE_NAME"));
        }
        for (var table : names) {
            var columns = new ArrayList<String>();
            try (var values = connection.getMetaData().getColumns(null, "dbo", table, "%")) {
                while (values.next()) {
                    String column = values.getString("COLUMN_NAME");
                    columns.add(column);
                }
            }
            result.put(table, read(connection, table, columns));
        }
        return result;
    }

    private static Snapshot read(Connection connection, String table, List<String> columns)
            throws SQLException {
        String projection =
                columns.stream()
                        .map(column -> "[" + column.replace("]", "]]") + "]")
                        .collect(java.util.stream.Collectors.joining(","));
        String query =
                "SELECT "
                        + projection
                        + " FROM [dbo].["
                        + table.replace("]", "]]")
                        + "] ORDER BY id FOR JSON PATH, INCLUDE_NULL_VALUES";
        var json = new StringBuilder();
        try (var sql = connection.createStatement();
                var rows = sql.executeQuery(query)) {
            while (rows.next()) json.append(rows.getString(1));
        }
        return new Snapshot(List.copyOf(columns), json.toString());
    }
}
