package it.gov.pagopa.onboarding.citizen.integration;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class OpenApiGenerationTest extends BaseIT {

    @Test
    void generateOpenApiFile() throws Exception {
        // 1. Chiama l'endpoint specifico per lo YAML (.yaml)
        byte[] result = webTestClient.get().uri("/v3/api-docs.yaml") 
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        if (result != null) {
            Path targetDir = Paths.get("target");
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            String yaml = new String(result, StandardCharsets.UTF_8);
            
            // 2. Salva con estensione .yaml
            Files.writeString(targetDir.resolve("openapi.yaml"), yaml);
            
            // Log per conferma
            System.out.println("✅ OpenAPI YAML generated at: " + targetDir.resolve("openapi.yaml").toAbsolutePath());
        } else {
            throw new IllegalStateException("OpenAPI response body was null");
        }
    }
}