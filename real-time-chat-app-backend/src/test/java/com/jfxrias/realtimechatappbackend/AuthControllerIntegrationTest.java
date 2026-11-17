package com.jfxrias.realtimechatappbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfxrias.realtimechatappbackend.dto.RegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    private static int sequence = 1000;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRegisterUser() throws Exception {
        String cpf = uniqueCpf();
        RegisterRequestDTO request = new RegisterRequestDTO("João", cpf, "123456", false);

        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/auth/register", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("João", body.get("name").asText());
        assertEquals(cpf, body.get("cpf").asText());
        assertEquals(false, body.get("isAdmin").asBoolean());
    }

    @Test
    void shouldLoginUser() throws Exception {
        String cpf = uniqueCpf();
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Maria", cpf, "123456", false);

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(baseUrl() + "/auth/register", registerRequest, String.class);
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                Map.of("cpf", cpf, "password", "123456"),
                String.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        JsonNode body = objectMapper.readTree(loginResponse.getBody());
        assertNotNull(body.get("token"));
        assertFalse(body.get("token").asText().isBlank());
        assertEquals(cpf, body.get("user").get("cpf").asText());
    }

    @Test
    void shouldRejectDuplicateCpf() throws Exception {
        String cpf = uniqueCpf();
        RegisterRequestDTO request = new RegisterRequestDTO("Pedro", cpf, "123456", false);

        ResponseEntity<String> firstResponse = restTemplate.postForEntity(baseUrl() + "/auth/register", request, String.class);
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());

        try {
            restTemplate.postForEntity(baseUrl() + "/auth/register", request, String.class);
            fail("Expected duplicate CPF to fail with 400");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            assertEquals("CPF já cadastrado", body.get("message").asText());
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String uniqueCpf() {
        return String.format("%011d", sequence++);
    }
}
