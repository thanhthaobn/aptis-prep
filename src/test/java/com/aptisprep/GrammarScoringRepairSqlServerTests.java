package com.aptisprep;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.common.enums.RecordStatus;
import com.aptisprep.mvc.scoring.entity.*;
import com.aptisprep.mvc.scoring.repository.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Exercises the explicit repair on disposable SQL Server databases, never application data. */
@EnabledIfEnvironmentVariable(named = "APTIS_SQLSERVER_ISOLATED_TESTS", matches = "true")
class GrammarScoringRepairSqlServerTests {
    private static final List<String> SKILLS =
            List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");

    @Test
    void repairVersionsPolicyPreservesHistoryAndClonesUsedExamsOnlyOnce() throws Exception {
        withDatabase(
                connection -> {
                    Fixture f = fixture(connection);
                    Map<String, String> history = history(connection);
                    String usedPractice = row(connection, "mock_exams", f.usedPractice());
                    String usedFull = row(connection, "mock_exams", f.usedFull());
                    String oldRules = json(connection, "scoring_scale_rules", "");
                    String oldBands = json(connection, "score_bands", "");

                    repair(connection);
                    int replacement =
                            scalar(
                                    connection,
                                    "SELECT id FROM scoring_schemes WHERE version='1.1-gv-c1'");
                    assertNotEquals(f.scheme(), replacement);
                    assertEquals(history, history(connection));
                    assertEquals(usedPractice, row(connection, "mock_exams", f.usedPractice()));
                    assertEquals(usedFull, row(connection, "mock_exams", f.usedFull()));
                    assertEquals(
                            oldRules,
                            json(
                                    connection,
                                    "scoring_scale_rules",
                                    "WHERE scoring_scheme_id=" + f.scheme()));
                    assertEquals(
                            oldBands,
                            json(
                                    connection,
                                    "score_bands",
                                    "WHERE scoring_scheme_id=" + f.scheme()));
                    for (int exam : List.of(f.practice(), f.placement(), f.checkpoint())) {
                        assertEquals(
                                replacement,
                                scalar(
                                        connection,
                                        "SELECT scoring_scheme_id FROM mock_exams WHERE id="
                                                + exam));
                    }
                    assertEquals(
                            f.scheme(),
                            scalar(
                                    connection,
                                    "SELECT scoring_scheme_id FROM mock_exams WHERE id=" + f.c2()));
                    assertEquals(
                            f.scheme(),
                            scalar(
                                    connection,
                                    "SELECT scoring_scheme_id FROM mock_exams WHERE id="
                                            + f.demo()));
                    assertEquals(9, scalar(connection, "SELECT COUNT(*) FROM mock_exams"));
                    int fullCopy =
                            scalar(
                                    connection,
                                    "SELECT id FROM mock_exams WHERE title LIKE '%[[]GV1.1 #"
                                            + f.usedFull()
                                            + "]' AND scoring_scheme_id="
                                            + replacement);
                    assertEquals(
                            5,
                            scalar(
                                    connection,
                                    "SELECT COUNT(*) FROM mock_exam_sections WHERE mock_exam_id="
                                            + fullCopy));
                    assertEquals(
                            5,
                            scalar(
                                    connection,
                                    "SELECT COUNT(*) FROM mock_exam_questions WHERE mock_exam_id="
                                            + fullCopy));
                    assertEquals(
                            0,
                            scalar(
                                    connection,
                                    "SELECT COUNT(*) FROM mock_exam_questions q JOIN mock_exam_sections s ON s.id=q.mock_exam_section_id WHERE q.mock_exam_id="
                                            + fullCopy
                                            + " AND s.mock_exam_id<>q.mock_exam_id"));

                    verifyPolicyFromStoredRows(connection, replacement);
                    insert(
                            connection,
                            "INSERT INTO exam_attempts(user_id,mock_exam_id,status,started_at,scoring_scheme_id,policy_version) OUTPUT INSERTED.id VALUES("
                                    + f.user()
                                    + ","
                                    + fullCopy
                                    + ",'IN_PROGRESS',SYSUTCDATETIME(),"
                                    + replacement
                                    + ",'scoring-scheme:"
                                    + replacement
                                    + ":1.1-gv-c1')");
                    Map<String, String> all = allRows(connection);
                    repair(connection);
                    assertEquals(
                            all,
                            allRows(connection),
                            "Rerun must not duplicate policy/exams or change history, even after a new attempt");
                });
    }

    @Test
    void incompleteOtherSkillRollsBackInsteadOfPublishingAPartialReplacement() throws Exception {
        withDatabase(
                connection -> {
                    fixture(connection);
                    execute(connection, "DELETE FROM score_bands WHERE skill='READING'");
                    Map<String, String> before = allRows(connection);
                    assertThrows(SQLException.class, () -> repair(connection));
                    assertEquals(before, allRows(connection));
                    assertEquals(0, scalar(connection, "SELECT @@TRANCOUNT"));
                });
    }

    @Test
    void rerunRefusesAnEditedReplacementInsteadOfOverwritingIt() throws Exception {
        withDatabase(
                connection -> {
                    fixture(connection);
                    repair(connection);
                    execute(
                            connection,
                            "UPDATE score_bands SET max_score=49.99 WHERE skill='GRAMMAR_VOCABULARY' AND cefr_level='C1' AND scoring_scheme_id IN (SELECT id FROM scoring_schemes WHERE version='1.1-gv-c1')");
                    Map<String, String> before = allRows(connection);
                    assertThrows(SQLException.class, () -> repair(connection));
                    assertEquals(before, allRows(connection));
                });
    }

    private void verifyPolicyFromStoredRows(Connection connection, int schemeId) throws Exception {
        var schemes = mock(ScoringSchemeRepository.class);
        var bands = mock(ScoreBandRepository.class);
        var rules = mock(ScoringScaleRuleRepository.class);
        var certificate = new Certificate();
        certificate.setStatus(RecordStatus.ACTIVE);
        var scheme = new ScoringScheme();
        scheme.setCertificate(certificate);
        scheme.setStatus(RecordStatus.ACTIVE);
        when(schemes.findById(schemeId)).thenReturn(Optional.of(scheme));
        for (String skill : SKILLS) {
            try (var sql = connection.createStatement();
                    var rows =
                            sql.executeQuery(
                                    "SELECT * FROM scoring_scale_rules WHERE scoring_scheme_id="
                                            + schemeId
                                            + " AND skill='"
                                            + skill
                                            + "'")) {
                assertTrue(rows.next());
                var rule = new ScoringScaleRule();
                rule.setRawMin(rows.getBigDecimal("raw_min"));
                rule.setRawMax(rows.getBigDecimal("raw_max"));
                rule.setScaleMin(rows.getBigDecimal("scale_min"));
                rule.setScaleMax(rows.getBigDecimal("scale_max"));
                when(rules.findBySchemeIdAndSkillIgnoreCase(schemeId, skill))
                        .thenReturn(Optional.of(rule));
            }
            var values = new ArrayList<ScoreBand>();
            try (var sql = connection.createStatement();
                    var rows =
                            sql.executeQuery(
                                    "SELECT * FROM score_bands WHERE scoring_scheme_id="
                                            + schemeId
                                            + " AND skill='"
                                            + skill
                                            + "' ORDER BY min_score")) {
                while (rows.next()) {
                    var band = new ScoreBand();
                    band.setCefrLevel(rows.getString("cefr_level"));
                    band.setMinScore(rows.getBigDecimal("min_score"));
                    band.setMaxScore(rows.getBigDecimal("max_score"));
                    values.add(band);
                }
            }
            when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(schemeId, skill))
                    .thenReturn(values);
        }
        var service =
                new com.aptisprep.mvc.scoring.service.impl.ScoringPolicyServiceImpl(
                        schemes, bands, rules);
        for (String skill : SKILLS) service.validatePolicy(schemeId, skill);
        for (String[] boundary :
                new String[][] {
                    {"0", "A1"},
                    {"9.99", "A1"},
                    {"10", "A2"},
                    {"19.99", "A2"},
                    {"20", "B1"},
                    {"29.99", "B1"},
                    {"30", "B2"},
                    {"39.99", "B2"},
                    {"40", "C1"},
                    {"50", "C1"}
                }) {
            assertEquals(
                    boundary[1],
                    service.estimatedLevel(
                            schemeId, "GRAMMAR_VOCABULARY", new BigDecimal(boundary[0])));
        }
        assertEquals(new BigDecimal("30.00"), service.raw(schemeId, "GRAMMAR_VOCABULARY", 3, 5));
        assertEquals(
                new BigDecimal("30.00"),
                service.scale(schemeId, "GRAMMAR_VOCABULARY", new BigDecimal("30")));
        assertThrows(
                com.aptisprep.mvc.common.exception.BusinessRuleException.class,
                () ->
                        service.estimatedLevel(
                                schemeId, "GRAMMAR_VOCABULARY", new BigDecimal("50.01")));
    }

    private Fixture fixture(Connection connection) throws Exception {
        int certificate =
                insert(
                        connection,
                        "INSERT INTO certificates(name,code,status) OUTPUT INSERTED.id VALUES('Internal estimate','APTIS_GENERAL','ACTIVE')");
        int scheme =
                insert(
                        connection,
                        "INSERT INTO scoring_schemes(certificate_id,name,version,status) OUTPUT INSERTED.id VALUES("
                                + certificate
                                + ",'Original estimate','1.0','ACTIVE')");
        int user =
                insert(
                        connection,
                        "INSERT INTO users(full_name,email,password_hash,role,status) OUTPUT INSERTED.id VALUES('Preserved learner','repair@test.invalid','unused','STUDENT','ACTIVE')");
        var questions = new LinkedHashMap<String, Integer>();
        for (String skill : SKILLS) {
            execute(
                    connection,
                    "INSERT INTO scoring_scale_rules(scoring_scheme_id,skill,raw_min,raw_max,scale_min,scale_max) VALUES("
                            + scheme
                            + ",'"
                            + skill
                            + "',0,"
                            + (Set.of("READING", "LISTENING").contains(skill) ? 40 : 50)
                            + ",0,50)");
            if (!skill.equals("GRAMMAR_VOCABULARY"))
                execute(
                        connection,
                        "INSERT INTO score_bands(scoring_scheme_id,skill,cefr_level,min_score,max_score) VALUES("
                                + scheme
                                + ",'"
                                + skill
                                + "','A1',0,50)");
            String type =
                    skill.equals("WRITING")
                            ? "WRITING_TEXT"
                            : skill.equals("SPEAKING") ? "SPEAKING_AUDIO" : "SINGLE_CHOICE";
            questions.put(
                    skill,
                    insert(
                            connection,
                            "INSERT INTO questions(content,question_type,skill,status) OUTPUT INSERTED.id VALUES('Preserved task','"
                                    + type
                                    + "','"
                                    + skill
                                    + "','PUBLISHED')"));
        }
        int practice =
                exam(connection, certificate, scheme, "PRACTICE", "B1", "Unused practice", false);
        int placement =
                exam(connection, certificate, scheme, "PLACEMENT", "B1", "Placement", false);
        int checkpoint =
                exam(connection, certificate, scheme, "CHECKPOINT", "B1", "Checkpoint", false);
        int usedPractice =
                exam(connection, certificate, scheme, "PRACTICE", "A1", "Used practice", false);
        int usedFull =
                exam(connection, certificate, scheme, "FULL_MOCK", "B1", "Used full mock", false);
        int c2 = exam(connection, certificate, scheme, "CHECKPOINT", "C2", "Legacy C2", false);
        int demo = exam(connection, certificate, scheme, "PRACTICE", "B1", "Demo", true);
        for (int exam : List.of(practice, placement, checkpoint, usedPractice, c2, demo)) {
            execute(
                    connection,
                    "INSERT INTO mock_exam_questions(mock_exam_id,question_id,position) VALUES("
                            + exam
                            + ","
                            + questions.get("GRAMMAR_VOCABULARY")
                            + ",1)");
        }
        int position = 0;
        for (String skill : SKILLS) {
            int section =
                    insert(
                            connection,
                            "INSERT INTO mock_exam_sections(mock_exam_id,skill,display_order,duration_minutes,status) OUTPUT INSERTED.id VALUES("
                                    + usedFull
                                    + ",'"
                                    + skill
                                    + "',"
                                    + (++position)
                                    + ",10,'ACTIVE')");
            execute(
                    connection,
                    "INSERT INTO mock_exam_questions(mock_exam_id,mock_exam_section_id,question_id,position) VALUES("
                            + usedFull
                            + ","
                            + section
                            + ","
                            + questions.get(skill)
                            + ","
                            + position
                            + ")");
        }
        for (int exam : List.of(usedPractice, usedFull)) {
            int attempt =
                    insert(
                            connection,
                            "INSERT INTO exam_attempts(user_id,mock_exam_id,status,grading_status,started_at,scoring_scheme_id,policy_version,final_scale_score,estimated_overall_cefr) OUTPUT INSERTED.id VALUES("
                                    + user
                                    + ","
                                    + exam
                                    + ",'COMPLETED','COMPLETED',SYSUTCDATETIME(),"
                                    + scheme
                                    + ",'original-policy',15,'A1')");
            execute(
                    connection,
                    "INSERT INTO exam_attempt_snapshots(exam_attempt_id,mock_exam_id,certificate_id,scoring_scheme_id,exam_type,title,answer_version,policy_version,grading_context,locked_question_snapshot) VALUES("
                            + attempt
                            + ","
                            + exam
                            + ","
                            + certificate
                            + ","
                            + scheme
                            + ",'PRACTICE','Original title',1,'original-policy','{}','[]')");
        }
        return new Fixture(
                scheme, user, practice, placement, checkpoint, usedPractice, usedFull, c2, demo);
    }

    private int exam(
            Connection connection,
            int certificate,
            int scheme,
            String type,
            String level,
            String title,
            boolean demo)
            throws SQLException {
        return insert(
                connection,
                "INSERT INTO mock_exams(certificate_id,scoring_scheme_id,title,exam_type,target_level,skill,duration_minutes,status,demo_only) OUTPUT INSERTED.id VALUES("
                        + certificate
                        + ","
                        + scheme
                        + ",'"
                        + title
                        + "','"
                        + type
                        + "','"
                        + level
                        + "','"
                        + (type.equals("FULL_MOCK") ? "FULL_MOCK" : "GRAMMAR_VOCABULARY")
                        + "',50,'PUBLISHED',"
                        + (demo ? 1 : 0)
                        + ")");
    }

    private void repair(Connection connection) throws Exception {
        execute(connection, Files.readString(Path.of("database/complete-grammar-scoring.sql")));
    }

    private Map<String, String> history(Connection connection) throws SQLException {
        var result = new LinkedHashMap<String, String>();
        for (String table :
                List.of(
                        "exam_attempts",
                        "exam_attempt_snapshots",
                        "exam_skill_results",
                        "user_answers",
                        "user_answer_details",
                        "user_skill_profiles",
                        "learning_paths",
                        "learning_path_items",
                        "lesson_progress",
                        "user_flashcard_progress")) result.put(table, json(connection, table, ""));
        return result;
    }

    private Map<String, String> allRows(Connection connection) throws SQLException {
        var result = new LinkedHashMap<String, String>();
        var tables = new ArrayList<String>();
        try (var sql = connection.createStatement();
                var rows =
                        sql.executeQuery(
                                "SELECT name FROM sys.tables WHERE is_ms_shipped=0 ORDER BY name")) {
            while (rows.next()) tables.add(rows.getString(1));
        }
        for (String table : tables) result.put(table, json(connection, table, ""));
        return result;
    }

    private String row(Connection connection, String table, int id) throws SQLException {
        return json(connection, table, "WHERE id=" + id);
    }

    private String json(Connection connection, String table, String filter) throws SQLException {
        var value = new StringBuilder();
        try (var sql = connection.createStatement();
                var rows =
                        sql.executeQuery(
                                "SELECT * FROM ["
                                        + table
                                        + "] "
                                        + filter
                                        + " ORDER BY id FOR JSON PATH, INCLUDE_NULL_VALUES")) {
            while (rows.next()) value.append(rows.getString(1));
        }
        return value.toString();
    }

    private int scalar(Connection connection, String query) throws SQLException {
        try (var sql = connection.createStatement();
                var rows = sql.executeQuery(query)) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }

    private int insert(Connection connection, String query) throws SQLException {
        return scalar(connection, query);
    }

    private void execute(Connection connection, String query) throws SQLException {
        try (var sql = connection.createStatement()) {
            sql.execute(query);
        }
    }

    private void withDatabase(DatabaseCheck check) throws Exception {
        String configured =
                System.getenv()
                        .getOrDefault(
                                "DB_URL",
                                "jdbc:sqlserver://localhost:1433;databaseName=aptisprep_db;encrypt=true;trustServerCertificate=true");
        String server = configured.replaceAll("(?i);databaseName=[^;]*", "");
        String login = System.getenv().getOrDefault("DB_USERNAME", "sa");
        String password = Objects.requireNonNull(System.getenv("DB_PASSWORD"));
        String database = "codex_grammar_test_" + UUID.randomUUID().toString().replace("-", "");
        assertTrue(database.matches("codex_grammar_test_[a-f0-9]{32}"));
        try (var master =
                DriverManager.getConnection(server + ";databaseName=master", login, password)) {
            execute(master, "CREATE DATABASE [" + database + "]");
            try (var connection =
                    DriverManager.getConnection(
                            server + ";databaseName=" + database, login, password)) {
                for (String batch :
                        Files.readString(Path.of("database/schema-current.sql"))
                                .split("(?im)^\\s*GO\\s*\\r?$")) {
                    if (!batch.isBlank()) execute(connection, batch);
                }
                check.run(connection);
            } finally {
                execute(
                        master,
                        "ALTER DATABASE ["
                                + database
                                + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                execute(master, "DROP DATABASE [" + database + "]");
            }
        }
    }

    private record Fixture(
            int scheme,
            int user,
            int practice,
            int placement,
            int checkpoint,
            int usedPractice,
            int usedFull,
            int c2,
            int demo) {}

    private interface DatabaseCheck {
        void run(Connection connection) throws Exception;
    }
}
