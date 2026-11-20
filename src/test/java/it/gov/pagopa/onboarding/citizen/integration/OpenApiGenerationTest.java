package it.gov.pagopa.onboarding.citizen.integration;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class OpenApiGenerationTest extends BaseIT {

    @Test
    void generateOpenApiFile() throws Exception {
        webTestClient.get().uri("/v3/api-docs")
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.paths['/emd/citizen/{fiscalCode}/{tppId}'].post.parameters[0].schema.minLength").isEqualTo(1)
                .jsonPath("$.paths['/emd/citizen/{fiscalCode}/{tppId}'].post.parameters[0].schema.maxLength").isEqualTo(100)
                .jsonPath("$.paths['/emd/citizen/search'].get.parameters[0].schema.minLength").isEqualTo(1)
                .jsonPath("$.paths['/emd/citizen/search'].get.parameters[0].schema.maxLength").isEqualTo(100)
                .jsonPath("$.components.schemas.CitizenConsentSearchRequest.properties.fiscalCode.minLength").isEqualTo(1)
                .jsonPath("$.components.schemas.CitizenConsentSearchRequest.properties.fiscalCode.maxLength").isEqualTo(100);
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