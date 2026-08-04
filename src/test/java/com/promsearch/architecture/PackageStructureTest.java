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
                .filter(path -> normalized(path).contains("/application/"))
                .toList();

        assertThat(applicationFiles)
                .filteredOn(path -> path.getFileName().toString().endsWith("UseCase.java"))
                .allMatch(path -> normalized(path).contains("/application/usecase/"));

        assertThat(applicationFiles)
                .filteredOn(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("Command.java")
                            || fileName.endsWith("Query.java")
                            || fileName.endsWith("Info.java");
                })
                .allMatch(path -> normalized(path).contains("/application/usecase/dto/"));

        assertThat(applicationFiles)
                .filteredOn(path -> path.getFileName().toString().endsWith("Service.java"))
                .allMatch(path -> normalized(path).contains("/application/service/command/")
                        || normalized(path).contains("/application/service/query/"));
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
                .allMatch(path -> normalized(path).contains("/infrastructure/persistence/"));

        assertThat(persistenceFiles)
                .filteredOn(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("JpaEntity.java") || fileName.endsWith("Id.java");
                })
                .allMatch(path -> normalized(path).contains("/infrastructure/persistence/entity/"));

        assertThat(persistenceFiles)
                .filteredOn(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("Repository.java")
                            || fileName.endsWith("Mapper.java")
                            || fileName.endsWith("PersistenceAdapter.java");
                })
                .noneMatch(path -> normalized(path).contains("/infrastructure/persistence/entity/"));

        assertThat(javaFiles())
                .noneMatch(path -> normalized(path).contains("/adapter/"));
    }

    @Test
    void interfaceDtosAreSeparatedIntoRequestAndResponsePackages() throws IOException {
        List<Path> interfaceDtoFiles = javaFiles().stream()
                .filter(path -> normalized(path).contains("/interfaces/dto/"))
                .toList();
        List<Path> responseFiles = interfaceDtoFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith("Response.java"))
                .toList();

        assertThat(interfaceDtoFiles)
                .filteredOn(path -> path.getFileName().toString().endsWith("Request.java"))
                .allMatch(path -> normalized(path).contains("/interfaces/dto/request/"));

        assertThat(responseFiles)
                .allMatch(path -> normalized(path).contains("/interfaces/dto/response/"));

        assertThat(interfaceDtoFiles)
                .noneMatch(path -> path.getParent().endsWith(Path.of("interfaces", "dto")));

        for (Path responseFile : responseFiles) {
            assertThat(Files.readString(responseFile))
                    .doesNotContain(".interfaces.dto.request");
        }
    }

    @Test
    void authBoundariesUsePortsAdaptersAndInboundUseCasesConsistently() throws IOException {
        List<Path> authFiles = javaFiles().stream()
                .filter(path -> normalized(path).contains("/auth/"))
                .toList();

        List<Path> authControllers = authFiles.stream()
                .filter(path -> normalized(path).contains("/interfaces/"))
                .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                .toList();
        for (Path controller : authControllers) {
            assertThat(Files.readString(controller))
                    .doesNotContain(".application.port.out.");
        }

        Path authenticationFilter = SOURCE_ROOT.resolve("global/security/JwtAuthenticationFilter.java");
        assertThat(Files.readString(authenticationFilter))
                .contains(".application.usecase.AuthenticateAccessTokenUseCase")
                .doesNotContain(".application.port.out.");

        List<Path> outboundInterfaces = authFiles.stream()
                .filter(path -> normalized(path).contains("/application/port/out/"))
                .filter(path -> {
                    try {
                        return Files.readString(path).contains("public interface ");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();
        assertThat(outboundInterfaces)
                .isNotEmpty()
                .allMatch(path -> path.getFileName().toString().endsWith("Port.java"));

        assertThat(authFiles)
                .filteredOn(path -> {
                    try {
                        String source = Files.readString(path);
                        return source.contains("import io.jsonwebtoken")
                                || source.contains("import org.springframework.web.client.RestClient");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .allMatch(path -> normalized(path).contains("/infrastructure/security/jwt/")
                        || normalized(path).contains("/infrastructure/external/oauth/"));

        assertThat(authFiles)
                .noneMatch(path -> normalized(path).contains("/infrastructure/jwt/")
                        || normalized(path).contains("/infrastructure/oauth/")
                        || normalized(path).contains("/infrastructure/crypto/")
                        || normalized(path).contains("/application/port/out/refresh/")
                        || normalized(path).contains("/application/port/out/social/"));
    }

    private List<Path> javaFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }
    }

    private String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
