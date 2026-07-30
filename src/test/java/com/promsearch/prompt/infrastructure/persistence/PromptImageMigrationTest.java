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

class PromptImageMigrationTest {

    private static final String[] MIGRATION_PATHS = {
            "db/migration/V3__create_prompt_images.sql",
            "db/migration/V4__add_prompt_image_upload_completion.sql"
    };

    @DisplayName("이미지 자산 마이그레이션은 PostgreSQL 호환 모드의 빈 DB에 적용된다")
    @Test
    void migrationCreatesPromptImagesTableAndConstraints() throws Exception {
        String databaseUrl = "jdbc:h2:mem:prompt_image_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
            executeMigration(connection);

            try (ResultSet columns = connection.getMetaData()
                    .getColumns(null, null, "prompt_images", "prompt_image_id")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("TYPE_NAME")).containsIgnoringCase("UUID");
            }

            execute(connection, validInsertSql());
            execute(connection, validUploadedInsertSql());

            assertThatThrownBy(() -> execute(connection, invalidWebpInsertSql()))
                    .isInstanceOf(SQLException.class);
        }
    }

    private void executeMigration(Connection connection) throws Exception {
        for (String migrationPath : MIGRATION_PATHS) {
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(migrationPath)) {
                assertThat(inputStream).as("migration resource").isNotNull();
                String migration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                for (String statement : migration.split(";")) {
                    if (!statement.isBlank()) {
                        execute(connection, statement);
                    }
                }
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String validInsertSql() {
        return """
                insert into prompt_images (
                    prompt_image_id, uploader_id, original_object_key, original_file_name,
                    content_type, file_size, width, height, status,
                    processing_version, is_thumbnail, lock_version, created_at, updated_at
                ) values (
                    random_uuid(), 1, 'originals/1/valid.jpg', 'valid.jpg',
                    'JPEG', 1024, 1920, 1080, 'UPLOADING',
                    0, false, 0, current_timestamp, current_timestamp
                )
                """;
    }

    private String invalidWebpInsertSql() {
        return """
                insert into prompt_images (
                    prompt_image_id, uploader_id, original_object_key, original_file_name,
                    content_type, file_size, width, height, status,
                    processing_version, is_thumbnail, lock_version, created_at, updated_at
                ) values (
                    random_uuid(), 1, 'originals/1/invalid.webp', 'invalid.webp',
                    'WEBP', 1024, 1920, 1080, 'UPLOADING',
                    0, false, 0, current_timestamp, current_timestamp
                )
                """;
    }

    private String validUploadedInsertSql() {
        return """
                insert into prompt_images (
                    prompt_image_id, uploader_id, original_object_key, original_file_name,
                    content_type, file_size, width, height, status, etag, uploaded_at,
                    processing_version, is_thumbnail, lock_version, created_at, updated_at
                ) values (
                    random_uuid(), 1, 'prompt-images/original/1/uploaded.jpg', 'uploaded.jpg',
                    'JPEG', 1024, 1920, 1080, 'UPLOADED', '"etag"', current_timestamp,
                    0, false, 0, current_timestamp, current_timestamp
                )
                """;
    }
}
