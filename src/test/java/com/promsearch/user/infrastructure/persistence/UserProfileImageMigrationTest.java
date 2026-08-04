package com.promsearch.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class UserProfileImageMigrationTest {

    private static final String MIGRATION_PATH =
            "db/migration/V8__add_user_profile_image_object_key.sql";

    @Test
    void addsProfileImageObjectKeyColumn() throws Exception {
        String databaseUrl = "jdbc:h2:mem:user_profile_image_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("create table users (user_id bigint primary key)");
            }

            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MIGRATION_PATH)) {
                assertThat(inputStream).isNotNull();
                String migration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                try (Statement statement = connection.createStatement()) {
                    statement.execute(migration);
                }
            }

            try (ResultSet columns = connection.getMetaData()
                    .getColumns(null, null, "users", "profile_image_object_key")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt("COLUMN_SIZE")).isEqualTo(500);
            }
        }
    }
}
