package com.aptisprep;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit local opt-in; never drops or cleans data. */
@EnabledIfEnvironmentVariable(named = "APTIS_SQLSERVER_TESTS", matches = "true")
class SqlServerMigrationTests {
    @Test
    void migrationsApplyAndExposeV2Columns() throws Exception {
        Properties local = new Properties();
        try (var input = Files.newInputStream(Path.of("src/main/resources/application-dev.properties"))) {
            local.load(input);
        }
        String url = local.getProperty("spring.datasource.url");
        String user = local.getProperty("spring.datasource.username");
        String password = local.getProperty("spring.datasource.password");
        var flyway = Flyway.configure().dataSource(url, user, password).cleanDisabled(true)
                .locations("classpath:db/migration").load();
        flyway.migrate();
        assertTrue(flyway.validateWithResult().validationSuccessful);
        try (var connection = DriverManager.getConnection(url, user, password)) {
            for (String column : new String[]{"text_answer", "audio_url", "ai_score", "ai_feedback", "graded_at"}) {
                try (var columns = connection.getMetaData().getColumns(null, "dbo", "user_answers", column)) {
                    assertTrue(columns.next(), column);
                }
            }
            try (var columns = connection.getMetaData().getColumns(null, "dbo", "mock_exam_questions", "mock_exam_id")) {
                assertTrue(columns.next());
            }
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                int certificate;
                try (var rows = statement.executeQuery("SELECT TOP 1 id FROM certificates ORDER BY id")) {
                    assertTrue(rows.next());
                    certificate = rows.getInt(1);
                }
                int firstExam;
                try (var rows = statement.executeQuery("INSERT INTO mock_exams (certificate_id,title,exam_type,status) OUTPUT INSERTED.id VALUES (" + certificate + ",N'V2 migration test','PRACTICE','DRAFT')")) {
                    rows.next(); firstExam = rows.getInt(1);
                }
                int secondExam;
                try (var rows = statement.executeQuery("INSERT INTO mock_exams (certificate_id,title,exam_type,status) OUTPUT INSERTED.id VALUES (" + certificate + ",N'V2 migration test 2','PRACTICE','DRAFT')")) {
                    rows.next(); secondExam = rows.getInt(1);
                }
                int question;
                try (var rows = statement.executeQuery("INSERT INTO questions (content,question_type,skill,status) OUTPUT INSERTED.id VALUES (N'V2 test','SINGLE_CHOICE','READING','DRAFT')")) {
                    rows.next(); question = rows.getInt(1);
                }
                statement.executeUpdate("INSERT INTO mock_exam_questions (mock_exam_id,question_id,position) VALUES (" + firstExam + "," + question + ",1)");
                statement.executeUpdate("INSERT INTO mock_exam_questions (mock_exam_id,question_id,position) VALUES (" + secondExam + "," + question + ",1)");
            } finally {
                connection.rollback();
            }
        }
    }
}
