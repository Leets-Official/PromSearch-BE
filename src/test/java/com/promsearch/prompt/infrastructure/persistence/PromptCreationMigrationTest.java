package com.promsearch.prompt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptCreationMigrationTest {

    private static final String MIGRATION_PATH = "db/migration/V6__add_prompt_creation_fields.sql";

    @DisplayName("생성 필드 마이그레이션은 공개 범위와 custom 태그 정규화 중복을 제한한다")
    @Test
    void migrationAddsPromptCreationConstraints() throws Exception {
        String databaseUrl = "jdbc:h2:mem:prompt_creation_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
            execute(connection, "create table posts (post_id bigint primary key)");
            execute(connection, """
                    create table tags (
                        tag_id bigint primary key,
                        tag_type varchar(20) not null,
                        tag_name varchar(100) not null,
                        normalized_name varchar(100)
                    )
                    """);
            execute(connection, """
                    insert into tags (tag_id, tag_type, tag_name, normalized_name)
                    values (1, 'AI_MODEL', 'GPT 4.1 Mini', null)
                    """);
            executeMigration(connection);

            execute(connection, "insert into posts (post_id) values (1)");
            try (ResultSet resultSet = query(
                    connection,
                    "select visibility from posts where post_id = 1"
            )) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("visibility")).isEqualTo("PUBLIC");
            }

            assertThatThrownBy(() -> execute(
                    connection,
                    "insert into posts (post_id, visibility) values (2, 'FRIENDS')"
            )).isInstanceOf(SQLException.class);

            try (ResultSet resultSet = query(
                    connection,
                    "select normalized_name from tags where tag_id = 1"
            )) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("normalized_name")).isEqualTo("gpt4.1mini");
            }

            execute(connection, """
                    insert into tags (tag_id, tag_type, tag_name, normalized_name)
                    values (2, 'AI_MODEL', 'Claude', 'claude')
                    """);
            assertThatThrownBy(() -> execute(connection, """
                    insert into tags (tag_id, tag_type, tag_name, normalized_name)
                    values (3, 'AI_MODEL', 'gpt4.1mini', 'gpt4.1mini')
                    """)).isInstanceOf(SQLException.class);
        }
    }

    private void executeMigration(Connection connection) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MIGRATION_PATH)) {
            assertThat(inputStream).as("migration resource").isNotNull();
            String migration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : migration.split(";")) {
                if (!statement.isBlank()) {
                    execute(connection, statement);
                }
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private ResultSet query(Connection connection, String sql) throws SQLException {
        return connection.createStatement().executeQuery(sql);
    }
}
