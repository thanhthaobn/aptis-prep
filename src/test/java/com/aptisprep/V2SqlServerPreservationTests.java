package com.aptisprep;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.dto.form.LessonForm;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import com.aptisprep.service.learning.LessonTeachingService;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Uses a generated, isolated database. Never migrates or cleans the configured application DB. */
@EnabledIfEnvironmentVariable(named = "APTIS_SQLSERVER_ISOLATED_TESTS", matches = "true")
class V2SqlServerPreservationTests {
    @Test
    void upgradeRetainsAllExistingRowsAndLegacyContent() throws Exception {
        Properties local = new Properties();
        try (var input =
                Files.newInputStream(Path.of("src/main/resources/application-dev.properties"))) {
            local.load(input);
        }
        String configured = local.getProperty("spring.datasource.url");
        String server = configured.replaceAll("(?i);databaseName=[^;]*", "");
        String login = local.getProperty("spring.datasource.username"),
                password = local.getProperty("spring.datasource.password");
        String name = "codex_v2_test_" + UUID.randomUUID().toString().replace("-", "");
        assertTrue(name.matches("codex_v2_test_[a-f0-9]{32}"));
        try (var master =
                DriverManager.getConnection(server + ";databaseName=master", login, password)) {
            master.createStatement().execute("CREATE DATABASE [" + name + "]");
            try {
                String url = server + ";databaseName=" + name;
                Flyway.configure()
                        .dataSource(url, login, password)
                        .cleanDisabled(true)
                        .target("37")
                        .load()
                        .migrate();
                Map<String, Snapshot> before;
                int answerId, jobId;
                try (var connection = DriverManager.getConnection(url, login, password);
                        var sql = connection.createStatement()) {
                    int user =
                            insert(
                                    sql,
                                    "INSERT INTO users(full_name,email,password_hash,role,status)"
                                        + " OUTPUT INSERTED.id VALUES(N'Preserved"
                                        + " learner','preserved@test.local','unused','STUDENT','ACTIVE')");
                    sql.executeUpdate(
                            "INSERT INTO lesson_engagement(user_id,lesson_id,audio_played_at)"
                                    + " SELECT TOP 1 "
                                    + user
                                    + ",id,SYSDATETIME() FROM lessons ORDER BY id");
                    assertEquals(
                            1,
                            sql.executeUpdate(
                                    "INSERT INTO"
                                        + " exam_attempts(user_id,mock_exam_id,status,started_at)"
                                        + " SELECT TOP 1 "
                                            + user
                                            + ",id,'COMPLETED',SYSDATETIME() FROM mock_exams WHERE"
                                            + " title = N'AptisPrep Demo · Writing · 2 câu AI'"));
                    int exam =
                            insert(
                                    sql,
                                    "INSERT INTO mock_exams(certificate_id,title,exam_type,status)"
                                            + " OUTPUT INSERTED.id SELECT TOP 1 id,N'Preserved"
                                            + " exam','PRACTICE','DRAFT' FROM certificates ORDER BY"
                                            + " id");
                    int question =
                            insert(
                                    sql,
                                    "INSERT INTO questions(content,question_type,skill,status)"
                                            + " OUTPUT INSERTED.id VALUES(N'Preserved"
                                            + " writing','WRITING_TEXT','WRITING','DRAFT')");
                    int item =
                            insert(
                                    sql,
                                    "INSERT INTO question_items(question_id,content,position)"
                                            + " OUTPUT INSERTED.id VALUES("
                                            + question
                                            + ",N'Preserved item',1)");
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
                    answerId =
                            insert(
                                    sql,
                                    "INSERT INTO user_answers(exam_attempt_id,question_id,status)"
                                            + " OUTPUT INSERTED.id VALUES("
                                            + attempt
                                            + ","
                                            + question
                                            + ",'ANSWERED')");
                    sql.execute(
                            "INSERT INTO"
                                + " user_answer_details(user_answer_id,question_item_id,answer_text,audio_path)"
                                + " VALUES("
                                    + answerId
                                    + ","
                                    + item
                                    + ",N'Original saved response',N'legacy/recording.wav')");
                    jobId =
                            insert(
                                    sql,
                                    "INSERT INTO"
                                        + " ai_assessment_jobs(assessment_key,user_id,attempt_id,answer_id,answer_version,skill,task_type,task_content,locked_content,rubric_version,policy_version,max_score,state)"
                                        + " OUTPUT INSERTED.id VALUES('lesson-1-submission-42',"
                                            + user
                                            + ",99,99,42,'WRITING','WRITING',N'Prompt',N'Original"
                                            + " answer','lesson-v1','learning-feedback-v1',50,'COMPLETED')");
                    // Explicit A/B/C fixtures ensure widening the unnamed CHECK preserves each key.
                    for (int correct = 0; correct < 3; correct++) {
                        sql.executeUpdate(sixOptionCheck(correct));
                    }
                    before = snapshot(connection);
                }
                // Also exercise an installation that already applied Lesson V039 before AI V040.
                Flyway.configure()
                        .dataSource(url, login, password)
                        .cleanDisabled(true)
                        .target("39")
                        .load()
                        .migrate();
                Map<String, Snapshot> beforeAiMigration;
                try (var connection = DriverManager.getConnection(url, login, password);
                        var sql = connection.createStatement()) {
                    sql.executeUpdate(
                            "UPDATE lesson_engagement SET playback_token_hash = 'retained-token', "
                                    + "playback_sequence = 7, playback_ranges_json = '[[0,45]]' "
                                    + "WHERE audio_played_at IS NOT NULL");
                    beforeAiMigration = snapshot(connection);
                }
                var flyway =
                        Flyway.configure()
                                .dataSource(url, login, password)
                                .cleanDisabled(true)
                                .load();
                flyway.migrate();
                assertTrue(flyway.validateWithResult().validationSuccessful);
                validateEntityMappings(url, login, password);
                try (var connection = DriverManager.getConnection(url, login, password);
                        var sql = connection.createStatement()) {
                    verifyDocumentedSchema(connection);
                    try (var demoRows =
                            sql.executeQuery(
                                    "SELECT COUNT(*) FROM mock_exams WHERE demo_only = 1")) {
                        assertTrue(demoRows.next());
                        assertEquals(3, demoRows.getInt(1));
                    }
                    try (var realRows =
                            sql.executeQuery(
                                    "SELECT demo_only FROM mock_exams WHERE title = N'Preserved"
                                        + " exam'")) {
                        assertTrue(realRows.next());
                        assertFalse(realRows.getBoolean(1));
                    }
                    for (var entry : before.entrySet()) {
                        var after = read(connection, entry.getKey(), entry.getValue().columns());
                        assertEquals(
                                entry.getValue().json(),
                                after.json(),
                                "Pre-existing data changed in " + entry.getKey());
                    }
                    for (var entry : beforeAiMigration.entrySet()) {
                        assertEquals(
                                entry.getValue().json(),
                                read(connection, entry.getKey(), entry.getValue().columns()).json(),
                                "V040 changed pre-existing data in " + entry.getKey());
                    }
                    try (var rows =
                            sql.executeQuery(
                                    "SELECT text_answer,audio_manifest FROM user_answers WHERE id="
                                            + answerId)) {
                        assertTrue(rows.next());
                        assertEquals("Original saved response", rows.getString(1));
                        assertEquals(
                                "legacy/recording.wav",
                                tools.jackson.databind.json.JsonMapper.builder()
                                        .build()
                                        .readTree(rows.getString(2))
                                        .get(0)
                                        .get("path")
                                        .asText());
                    }
                    try (var rows =
                            sql.executeQuery(
                                    "SELECT source_type,source_id,attempt_id,answer_id FROM"
                                            + " ai_assessment_jobs WHERE id="
                                            + jobId)) {
                        assertTrue(rows.next());
                        assertEquals("LESSON_SUBMISSION", rows.getString(1));
                        assertEquals(42, rows.getInt(2));
                        assertEquals(99, rows.getInt(3));
                        assertEquals(99, rows.getInt(4));
                    }
                    for (int correct = 3; correct <= 5; correct++)
                        assertEquals(1, sql.executeUpdate(sixOptionCheck(correct)));
                    assertThrows(SQLException.class, () -> sql.executeUpdate(sixOptionCheck(6)));
                    assertThrows(SQLException.class, () -> sql.executeUpdate(sixOptionCheck(-1)));
                }
                verifyKnowledgeCheckService(url, login, password);
            } finally {
                // The only drop target is the validated UUID-named database created by this test.
                master.createStatement()
                        .execute(
                                "ALTER DATABASE ["
                                        + name
                                        + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                master.createStatement().execute("DROP DATABASE [" + name + "]");
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
                            new LessonTeachingService(
                                    checks,
                                    lessons,
                                    users,
                                    repositories.getRepository(LessonResponseRepository.class),
                                    attempts,
                                    repositories.getRepository(LessonEngagementRepository.class),
                                    null,
                                    repositories.getRepository(LessonProgressRepository.class),
                                    null);
                    // Storage and AI are intentionally unused by Knowledge Check operations.
                    var lesson = new com.aptisprep.entity.Lesson();
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
                    em.getTransaction().commit();
                } finally {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                }
            }
        } finally {
            if (factory.getObject() != null) factory.destroy();
        }
    }

    private static LessonForm checkForm(int count, int correct) {
        var form = new LessonForm();
        form.setSkill("Reading");
        form.setStatus("PUBLISHED");
        var check = new LessonForm.CheckForm();
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

    private static void validateEntityMappings(String url, String login, String password) {
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
                    // This column is deliberately backfilled from the preserved item data.
                    if (table.equals("user_answers") && column.equals("text_answer")) continue;
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
