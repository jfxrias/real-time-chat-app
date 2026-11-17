package com.jfxrias.realtimechatappbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfxrias.realtimechatappbackend.dto.RegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GroupControllerIntegrationTest {
    private static int sequence = 2000;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateGroupWithCreatorUuid() throws Exception {
        String creatorUuid = registerUser("Criador");
        Map<String, Object> request = Map.of(
                "groupName", "Grupo do João",
                "creatorUuid", creatorUuid
        );

        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/groups", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Grupo do João", body.get("groupName").asText());
        assertEquals(creatorUuid, body.get("creatorUuid").asText());
        assertNotNull(body.get("groupUuid"));
    }

    @Test
    void shouldDeleteOnlyGroupCreator() throws Exception {
        String creatorUuid = registerUser("Criador");
        String otherUserUuid = registerUser("Outro");

        Map<String, Object> request = Map.of(
                "groupName", "Grupo de teste",
                "creatorUuid", creatorUuid
        );

        ResponseEntity<String> createResponse = restTemplate.postForEntity(baseUrl() + "/groups", request, String.class);
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        String groupUuid = objectMapper.readTree(createResponse.getBody()).get("groupUuid").asText();

        HttpHeaders headers = new HttpHeaders();
        headers.set("user-uuid", otherUserUuid);
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(baseUrl() + "/groups/" + groupUuid, HttpMethod.DELETE, deleteRequest, String.class);
            fail("Expected non-creator to be denied");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            assertEquals("Apenas o criador pode excluir o grupo", body.get("message").asText());
        }

        HttpHeaders creatorHeaders = new HttpHeaders();
        creatorHeaders.set("user-uuid", creatorUuid);
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl() + "/groups/" + groupUuid,
                HttpMethod.DELETE,
                new HttpEntity<>(creatorHeaders),
                String.class
        );

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        JsonNode deleteBody = objectMapper.readTree(deleteResponse.getBody());
        assertEquals("Grupo excluído com sucesso", deleteBody.get("message").asText());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String registerUser(String name) {
        String cpf = String.format("%011d", sequence++);
        RegisterRequestDTO request = new RegisterRequestDTO(name, cpf, "123456", false);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/auth/register", request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = null;
        try {
            body = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse register response", e);
        }
        return body.get("userUuid").asText();
    }
}
