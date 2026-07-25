package com.promsearch.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PackageStructureTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/promsearch");

    @Test
    void applicationTypesFollowUseCaseAndServiceFolders() throws IOException {
        List<Path> applicationFiles = javaFiles().stream()
                .filter(path -> path.toString().contains("/application/"))
                .toList();

        assertThat(applicationFiles)
                .filteredOn(path -> path.getFileName().toString().endsWith("UseCase.java"))
                .allMatch(path -> path.toString().contains("/application/usecase/"));

        assertThat(applicationFiles)
                .filteredOn(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("Command.java")
                            || fileName.endsWith("Query.java")
                            || fileName.endsWith("Info.java");
                })
                .allMatch(path -> path.toString().contains("/application/usecase/dto/"));

        assertThat(applicationFiles)
                .filteredOn(path -> path.getFileName().toString().endsWith("Service.java"))
                .allMatch(path -> path.toString().contains("/application/service/command/")
                        || path.toString().contains("/application/service/query/"));
    }

    @Test
    void persistenceTypesStayInsideInfrastructure() throws IOException {
        List<Path> persistenceFiles = javaFiles().stream()
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("JpaEntity.java")
                            || fileName.endsWith("Id.java")
                            || fileName.endsWith("Repository.java")
                            || fileName.endsWith("Mapper.java")
                            || fileName.endsWith("PersistenceAdapter.java");
                })
                .toList();

        assertThat(persistenceFiles)
                .isNotEmpty()
                .allMatch(path -> path.toString().contains("/infrastructure/persistence/"));

        assertThat(persistenceFiles)
                .filteredOn(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("JpaEntity.java") || fileName.endsWith("Id.java");
                })
                .allMatch(path -> path.toString().contains("/infrastructure/persistence/entity/"));

        assertThat(persistenceFiles)
                .filteredOn(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("Repository.java")
                            || fileName.endsWith("Mapper.java")
                            || fileName.endsWith("PersistenceAdapter.java");
                })
                .noneMatch(path -> path.toString().contains("/infrastructure/persistence/entity/"));

        assertThat(javaFiles())
                .noneMatch(path -> path.toString().contains("/adapter/"));
    }

    @Test
    void interfaceDtosAreSeparatedIntoRequestAndResponsePackages() throws IOException {
        List<Path> interfaceDtoFiles = javaFiles().stream()
                .filter(path -> path.toString().contains("/interfaces/dto/"))
                .toList();
        List<Path> responseFiles = interfaceDtoFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith("Response.java"))
                .toList();

        assertThat(interfaceDtoFiles)
                .filteredOn(path -> path.getFileName().toString().endsWith("Request.java"))
                .allMatch(path -> path.toString().contains("/interfaces/dto/request/"));

        assertThat(responseFiles)
                .allMatch(path -> path.toString().contains("/interfaces/dto/response/"));

        assertThat(interfaceDtoFiles)
                .noneMatch(path -> path.getParent().endsWith(Path.of("interfaces", "dto")));

        for (Path responseFile : responseFiles) {
            assertThat(Files.readString(responseFile))
                    .doesNotContain(".interfaces.dto.request");
        }
    }

    private List<Path> javaFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }
    }
}
