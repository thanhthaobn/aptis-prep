package com.aptisprep;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Legacy test name retained for scripts; now read-only validation, never migration. */
@EnabledIfEnvironmentVariable(named = "APTIS_SQLSERVER_TESTS", matches = "true")
class SqlServerMigrationTests {
    @Test
    void existingDatabaseMatchesEntitiesWithoutChangingSchema() throws Exception {
        String url = System.getenv("DB_URL");
        String user = System.getenv().getOrDefault("DB_USERNAME", "sa");
        String password = System.getenv("DB_PASSWORD");
        if (url == null) throw new IllegalStateException("Set DB_URL before validating a database");
        V2SqlServerPreservationTests.validateEntityMappings(url, user, password);
    }
}
