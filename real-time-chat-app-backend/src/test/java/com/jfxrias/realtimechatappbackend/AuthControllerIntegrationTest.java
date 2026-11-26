package com.jfxrias.realtimechatappbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfxrias.realtimechatappbackend.dto.RegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

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

    @Test
    void shouldUpdateAuthenticatedUserName() throws Exception {
        String cpf = uniqueCpf();
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Carlos", cpf, "123456", false);
        restTemplate.postForEntity(baseUrl() + "/auth/register", registerRequest, String.class);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                Map.of("cpf", cpf, "password", "123456"),
                String.class
        );
        JsonNode loginBody = objectMapper.readTree(loginResponse.getBody());
        String token = loginBody.get("token").asText();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(Map.of("name", "Carlos Atualizado"), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/auth/name",
                HttpMethod.PATCH,
                requestEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Carlos Atualizado", body.get("name").asText());
        assertEquals(cpf, body.get("cpf").asText());
    }

    @Test
    void shouldUpdateAuthenticatedUserPassword() throws Exception {
        String cpf = uniqueCpf();
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("Ana", cpf, "123456", false);
        restTemplate.postForEntity(baseUrl() + "/auth/register", registerRequest, String.class);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                Map.of("cpf", cpf, "password", "123456"),
                String.class
        );
        JsonNode loginBody = objectMapper.readTree(loginResponse.getBody());
        String token = loginBody.get("token").asText();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Map<String, String>> updatePasswordRequest = new HttpEntity<>(Map.of("password", "654321"), headers);

        ResponseEntity<String> updatePasswordResponse = restTemplate.exchange(
                baseUrl() + "/auth/password",
                HttpMethod.PATCH,
                updatePasswordRequest,
                String.class
        );
        assertEquals(HttpStatus.OK, updatePasswordResponse.getStatusCode());
        JsonNode updatePasswordBody = objectMapper.readTree(updatePasswordResponse.getBody());
        assertEquals("Senha atualizada com sucesso", updatePasswordBody.get("message").asText());

        ResponseEntity<String> reloginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                Map.of("cpf", cpf, "password", "654321"),
                String.class
        );
        assertEquals(HttpStatus.OK, reloginResponse.getStatusCode());
        JsonNode reloginBody = objectMapper.readTree(reloginResponse.getBody());
        assertNotNull(reloginBody.get("token"));
        assertFalse(reloginBody.get("token").asText().isBlank());
    }

    @Test
    void shouldRejectNameUpdateWithoutToken() throws Exception {
        try {
            restTemplate.exchange(
                    baseUrl() + "/auth/name",
                    HttpMethod.PATCH,
                    new HttpEntity<>(Map.of("name", "Sem Token")),
                    String.class
            );
            fail("Expected missing token to fail with 401");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            assertEquals("Token não informado", body.get("message").asText());
        }
    }

    @Test
    void shouldCreateFriendshipBetweenAuthenticatedAndTargetUser() throws Exception {
        String requesterCpf = uniqueCpf();
        String targetCpf = uniqueCpf();
        restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new RegisterRequestDTO("Solicitante", requesterCpf, "123456", false),
                String.class
        );
        ResponseEntity<String> targetRegisterResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new RegisterRequestDTO("Alvo", targetCpf, "123456", false),
                String.class
        );
        JsonNode targetRegisterBody = objectMapper.readTree(targetRegisterResponse.getBody());
        String targetUserUuid = targetRegisterBody.get("userUuid").asText();

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                Map.of("cpf", requesterCpf, "password", "123456"),
                String.class
        );
        String token = objectMapper.readTree(loginResponse.getBody()).get("token").asText();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(Map.of("userUuid", targetUserUuid), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/auth/friendships",
                requestEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Amizade criada com sucesso", body.get("message").asText());
        assertNotNull(body.get("firstUserUuid"));
        assertNotNull(body.get("secondUserUuid"));
        assertFalse(body.get("firstUserUuid").asText().isBlank());
        assertFalse(body.get("secondUserUuid").asText().isBlank());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String uniqueCpf() {
        return String.format("%011d", sequence++);
    }
}
