package com.aptisprep;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Uses a generated, isolated database. Never migrates or cleans the configured application DB. */
@EnabledIfEnvironmentVariable(named = "APTIS_SQLSERVER_ISOLATED_TESTS", matches = "true")
class V2SqlServerPreservationTests {
    @Test void upgradeRetainsAllExistingRowsAndLegacyContent() throws Exception {
        Properties local = new Properties();
        try (var input = Files.newInputStream(Path.of("src/main/resources/application-dev.properties"))) { local.load(input); }
        String configured = local.getProperty("spring.datasource.url");
        String server = configured.replaceAll("(?i);databaseName=[^;]*", "");
        String login=local.getProperty("spring.datasource.username"), password=local.getProperty("spring.datasource.password");
        String name="codex_v2_test_"+UUID.randomUUID().toString().replace("-", "");
        assertTrue(name.matches("codex_v2_test_[a-f0-9]{32}"));
        try (var master=DriverManager.getConnection(server+";databaseName=master",login,password)) {
            master.createStatement().execute("CREATE DATABASE ["+name+"]");
            try {
                String url=server+";databaseName="+name;
                Flyway.configure().dataSource(url,login,password).cleanDisabled(true).target("37").load().migrate();
                Map<String, Snapshot> before;
                int answerId,jobId;
                try(var connection=DriverManager.getConnection(url,login,password);var sql=connection.createStatement()) {
                    int user=insert(sql,"INSERT INTO users(full_name,email,password_hash,role,status) OUTPUT INSERTED.id VALUES(N'Preserved learner','preserved@test.local','unused','STUDENT','ACTIVE')");
                    sql.executeUpdate("INSERT INTO lesson_engagement(user_id,lesson_id,audio_played_at) SELECT TOP 1 "+user+",id,SYSDATETIME() FROM lessons ORDER BY id");
                    int exam=insert(sql,"INSERT INTO mock_exams(certificate_id,title,exam_type,status) OUTPUT INSERTED.id SELECT TOP 1 id,N'Preserved exam','PRACTICE','DRAFT' FROM certificates ORDER BY id");
                    int question=insert(sql,"INSERT INTO questions(content,question_type,skill,status) OUTPUT INSERTED.id VALUES(N'Preserved writing','WRITING_TEXT','WRITING','DRAFT')");
                    int item=insert(sql,"INSERT INTO question_items(question_id,content,position) OUTPUT INSERTED.id VALUES("+question+",N'Preserved item',1)");
                    int attempt=insert(sql,"INSERT INTO exam_attempts(user_id,mock_exam_id,status,started_at) OUTPUT INSERTED.id VALUES("+user+","+exam+",'IN_PROGRESS',SYSDATETIME())");
                    answerId=insert(sql,"INSERT INTO user_answers(exam_attempt_id,question_id,status) OUTPUT INSERTED.id VALUES("+attempt+","+question+",'ANSWERED')");
                    sql.execute("INSERT INTO user_answer_details(user_answer_id,question_item_id,answer_text,audio_path) VALUES("+answerId+","+item+",N'Original saved response',N'legacy/recording.wav')");
                    jobId=insert(sql,"INSERT INTO ai_assessment_jobs(assessment_key,user_id,attempt_id,answer_id,answer_version,skill,task_type,task_content,locked_content,rubric_version,policy_version,max_score,state) OUTPUT INSERTED.id VALUES('lesson-1-submission-42',"+user+",99,99,42,'WRITING','WRITING',N'Prompt',N'Original answer','lesson-v1','learning-feedback-v1',50,'COMPLETED')");
                    before=snapshot(connection);
                }
                var flyway=Flyway.configure().dataSource(url,login,password).cleanDisabled(true).load();
                flyway.migrate();assertTrue(flyway.validateWithResult().validationSuccessful);
                validateEntityMappings(url,login,password);
                try(var connection=DriverManager.getConnection(url,login,password);var sql=connection.createStatement()) {
                    for(var entry:before.entrySet()) {
                        var after=read(connection,entry.getKey(),entry.getValue().columns());
                        assertEquals(entry.getValue().json(),after.json(),"Pre-existing data changed in "+entry.getKey());
                    }
                    try(var rows=sql.executeQuery("SELECT text_answer,audio_manifest FROM user_answers WHERE id="+answerId)) {
                        assertTrue(rows.next());assertEquals("Original saved response",rows.getString(1));
                        assertEquals("legacy/recording.wav",tools.jackson.databind.json.JsonMapper.builder().build().readTree(rows.getString(2)).get(0).get("path").asText());
                    }
                    try(var rows=sql.executeQuery("SELECT source_type,source_id,attempt_id,answer_id FROM ai_assessment_jobs WHERE id="+jobId)) {
                        assertTrue(rows.next());assertEquals("LESSON_SUBMISSION",rows.getString(1));assertEquals(42,rows.getInt(2));assertEquals(99,rows.getInt(3));assertEquals(99,rows.getInt(4));
                    }
                    for (int correct=3;correct<=5;correct++) assertEquals(1,sql.executeUpdate(sixOptionCheck(correct)));
                    assertThrows(SQLException.class,()->sql.executeUpdate(sixOptionCheck(6)));
                }
            } finally {
                // The only drop target is the validated UUID-named database created by this test.
                master.createStatement().execute("ALTER DATABASE ["+name+"] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                master.createStatement().execute("DROP DATABASE ["+name+"]");
            }
        }
    }
    private static int insert(Statement sql,String query) throws SQLException {
        try(var rows=sql.executeQuery(query)){assertTrue(rows.next());return rows.getInt(1);}
    }
    private static String sixOptionCheck(int correct) {
        return "INSERT INTO lesson_checks(lesson_id,position,prompt,option_a,option_b,option_c,option_d,option_e,option_f,correct_option,explanation) "
                +"SELECT TOP 1 id,"+(9000+correct)+",N'Six options',N'A',N'B',N'C',N'D',N'E',N'F',"+correct+",N'Explanation' FROM lessons ORDER BY id";
    }
    private static void validateEntityMappings(String url,String login,String password) {
        var factory=new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new org.springframework.jdbc.datasource.DriverManagerDataSource(url,login,password));
        factory.setPackagesToScan("com.aptisprep.entity");
        factory.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));
        try { factory.afterPropertiesSet();assertNotNull(factory.getObject()); }
        finally { if(factory.getObject()!=null)factory.destroy(); }
    }
    private record Snapshot(List<String> columns,String json) {}
    private static Map<String,Snapshot> snapshot(Connection connection) throws SQLException {
        var result=new LinkedHashMap<String,Snapshot>();
        var names=new ArrayList<String>();
        try(var tables=connection.getMetaData().getTables(null,"dbo","%",new String[]{"TABLE"})) {
            while(tables.next())if(!tables.getString("TABLE_NAME").equals("flyway_schema_history"))names.add(tables.getString("TABLE_NAME"));
        }
        for(var table:names) {
            var columns=new ArrayList<String>();
            try(var values=connection.getMetaData().getColumns(null,"dbo",table,"%")) {
                while(values.next()) {
                    String column=values.getString("COLUMN_NAME");
                    // This column is deliberately backfilled from the preserved item data.
                    if(table.equals("user_answers")&&column.equals("text_answer"))continue;
                    columns.add(column);
                }
            }
            result.put(table,read(connection,table,columns));
        }
        return result;
    }
    private static Snapshot read(Connection connection,String table,List<String> columns) throws SQLException {
        String projection=columns.stream().map(column->"["+column.replace("]","]]")+"]").collect(java.util.stream.Collectors.joining(","));
        String query="SELECT "+projection+" FROM [dbo].["+table.replace("]","]]")+"] ORDER BY id FOR JSON PATH, INCLUDE_NULL_VALUES";
        var json=new StringBuilder();try(var sql=connection.createStatement();var rows=sql.executeQuery(query)){while(rows.next())json.append(rows.getString(1));}
        return new Snapshot(List.copyOf(columns),json.toString());
    }
}
