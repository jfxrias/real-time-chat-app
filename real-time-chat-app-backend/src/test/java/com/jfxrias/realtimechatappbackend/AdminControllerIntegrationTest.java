package com.jfxrias.realtimechatappbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminControllerIntegrationTest {
    private static int sequence = 4000;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeactivateAndReactivateUserWhenPlatformAdmin() throws Exception {
        TestUser admin = registerAndLogin("Admin plataforma", true);
        TestUser target = registerAndLogin("Usuário alvo", false);

        ResponseEntity<String> deactivateResponse = restTemplate.exchange(
                baseUrl() + "/admin/users/" + target.uuid() + "/deactivate",
                HttpMethod.PATCH,
                authenticatedRequest(Map.of(), admin.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, deactivateResponse.getStatusCode());
        JsonNode deactivateBody = objectMapper.readTree(deactivateResponse.getBody());
        assertEquals("Conta desativada com sucesso", deactivateBody.get("message").asText());
        assertFalse(deactivateBody.get("isActive").asBoolean());

        ResponseEntity<String> reactivateResponse = restTemplate.exchange(
                baseUrl() + "/admin/users/" + target.uuid() + "/reactivate",
                HttpMethod.PATCH,
                authenticatedRequest(Map.of(), admin.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, reactivateResponse.getStatusCode());
        JsonNode reactivateBody = objectMapper.readTree(reactivateResponse.getBody());
        assertEquals("Conta reativada com sucesso", reactivateBody.get("message").asText());
        assertTrue(reactivateBody.get("isActive").asBoolean());
    }

    @Test
    void shouldRejectUserStatusChangeWhenRequesterIsNotPlatformAdmin() {
        TestUser regularUser = registerAndLogin("Usuário comum", false);
        TestUser target = registerAndLogin("Outro usuário", false);

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/admin/users/" + target.uuid() + "/deactivate",
                        HttpMethod.PATCH,
                        authenticatedRequest(Map.of(), regularUser.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Apenas administradores da plataforma podem realizar esta operação"
        );
    }

    @Test
    void shouldDeactivateAndReactivateGroupWhenPlatformAdmin() throws Exception {
        TestUser admin = registerAndLogin("Admin grupos", true);
        TestUser creator = registerAndLogin("Criador grupo", false);
        String groupUuid = createGroup(creator, "Grupo administrativo");

        ResponseEntity<String> deactivateResponse = restTemplate.exchange(
                baseUrl() + "/admin/groups/" + groupUuid + "/deactivate",
                HttpMethod.PATCH,
                authenticatedRequest(Map.of(), admin.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, deactivateResponse.getStatusCode());
        JsonNode deactivateBody = objectMapper.readTree(deactivateResponse.getBody());
        assertEquals("Grupo desativado com sucesso", deactivateBody.get("message").asText());
        assertEquals("INACTIVE", deactivateBody.get("status").asText());

        ResponseEntity<String> reactivateResponse = restTemplate.exchange(
                baseUrl() + "/admin/groups/" + groupUuid + "/reactivate",
                HttpMethod.PATCH,
                authenticatedRequest(Map.of(), admin.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, reactivateResponse.getStatusCode());
        JsonNode reactivateBody = objectMapper.readTree(reactivateResponse.getBody());
        assertEquals("Grupo reativado com sucesso", reactivateBody.get("message").asText());
        assertEquals("ACTIVE", reactivateBody.get("status").asText());
    }

    @Test
    void shouldRejectAdminEndpointsWithoutToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(), headers);

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/admin/users/qualquer/deactivate",
                        HttpMethod.PATCH,
                        request,
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Token não informado"
        );
    }

    @Test
    void shouldListAdminResourcesAndReportsWhenAdminAndBlockRegularUser() throws Exception {
        TestUser admin = registerAndLogin("Admin listagens", true);
        TestUser regular = registerAndLogin("Usuário regular", false);
        TestUser reported = registerAndLogin("Usuário reportado", false);
        String groupUuid = createGroup(regular, "Grupo report");

        restTemplate.exchange(
                baseUrl() + "/reports",
                HttpMethod.POST,
                authenticatedRequest(Map.of(
                        "reportedUuid", reported.uuid(),
                        "groupUuid", groupUuid,
                        "reason", "Teste admin reports"
                ), regular.token()),
                String.class
        );

        ResponseEntity<String> usersResponse = restTemplate.exchange(
                baseUrl() + "/admin/users",
                HttpMethod.GET,
                authenticatedRequest(Map.of(), admin.token()),
                String.class
        );
        assertEquals(HttpStatus.OK, usersResponse.getStatusCode());
        assertTrue(objectMapper.readTree(usersResponse.getBody()).isArray());

        ResponseEntity<String> groupsResponse = restTemplate.exchange(
                baseUrl() + "/admin/groups",
                HttpMethod.GET,
                authenticatedRequest(Map.of(), admin.token()),
                String.class
        );
        assertEquals(HttpStatus.OK, groupsResponse.getStatusCode());
        assertTrue(objectMapper.readTree(groupsResponse.getBody()).isArray());

        ResponseEntity<String> reportsResponse = restTemplate.exchange(
                baseUrl() + "/reports",
                HttpMethod.GET,
                authenticatedRequest(Map.of(), admin.token()),
                String.class
        );
        assertEquals(HttpStatus.OK, reportsResponse.getStatusCode());
        JsonNode reportsBody = objectMapper.readTree(reportsResponse.getBody());
        assertTrue(reportsBody.isArray());
        assertTrue(reportsBody.size() >= 1);

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/admin/users",
                        HttpMethod.GET,
                        authenticatedRequest(Map.of(), regular.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Apenas administradores da plataforma podem realizar esta operação"
        );

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/reports",
                        HttpMethod.GET,
                        authenticatedRequest(Map.of(), regular.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Apenas administradores da plataforma podem realizar esta operação"
        );
    }

    private String createGroup(TestUser creator, String name) {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/groups",
                HttpMethod.POST,
                authenticatedRequest(Map.of("groupName", name + uniqueSuffix(), "creatorUuid", creator.uuid()), creator.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        try {
            return objectMapper.readTree(response.getBody()).get("groupUuid").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestUser registerAndLogin(String name, boolean isAdmin) {
        String cpf = String.format("%011d", sequence++);

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                Map.of("name", name, "cpf", cpf, "password", "123456", "isAdmin", isAdmin),
                String.class
        );
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());

        try {
            JsonNode registerBody = objectMapper.readTree(registerResponse.getBody());
            String uuid = registerBody.get("userUuid").asText();

            ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                    baseUrl() + "/auth/login",
                    Map.of("cpf", cpf, "password", "123456"),
                    String.class
            );
            assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

            JsonNode loginBody = objectMapper.readTree(loginResponse.getBody());
            return new TestUser(uuid, loginBody.get("token").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpEntity<Map<String, Object>> authenticatedRequest(Map<String, Object> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private void assertError(org.junit.jupiter.api.function.Executable executable, HttpStatus status, String message) {
        try {
            executable.execute();
            fail("Era esperado erro HTTP " + status);
        } catch (HttpClientErrorException ex) {
            assertEquals(status, ex.getStatusCode());
            String responseBody = ex.getResponseBodyAsString();
            assertTrue(
                    responseBody != null && responseBody.contains(message),
                    "Resposta esperada conter: " + message + ". Resposta recebida: " + responseBody
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String uniqueSuffix() {
        return " - " + System.nanoTime();
    }

    private record TestUser(String uuid, String token) {
    }
}
