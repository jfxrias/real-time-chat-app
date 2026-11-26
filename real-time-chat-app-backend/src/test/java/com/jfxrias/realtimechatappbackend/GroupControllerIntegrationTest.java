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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GroupControllerIntegrationTest {
    private static int sequence = 2000;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
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

    @Test
    void shouldRemoveMemberWhenRequesterIsGroupCreator() throws Exception {
        TestUser creator = registerAndLogin("Criador remoção");
        TestUser member = registerAndLogin("Membro remoção");

        String groupUuid = createGroup(creator.uuid());
        addMemberToGroup(groupUuid, creator.token(), member.uuid(), "MEMBER");

        ResponseEntity<String> removeResponse = restTemplate.exchange(
                baseUrl() + "/groups/" + groupUuid + "/members/" + member.uuid(),
                HttpMethod.DELETE,
                authenticatedRequest(Map.of(), creator.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, removeResponse.getStatusCode());
        JsonNode body = objectMapper.readTree(removeResponse.getBody());
        assertEquals("Usuário removido do grupo", body.get("message").asText());
        assertEquals(groupUuid, body.get("groupUuid").asText());
        assertEquals(member.uuid(), body.get("userUuid").asText());
    }

    @Test
    void shouldRejectMemberRemovalWhenRequesterIsNotCreatorOrAdmin() throws Exception {
        TestUser creator = registerAndLogin("Criador sem permissão externa");
        TestUser member = registerAndLogin("Membro alvo");
        TestUser outsider = registerAndLogin("Usuário externo");

        String groupUuid = createGroup(creator.uuid());
        addMemberToGroup(groupUuid, creator.token(), member.uuid(), "MEMBER");

        try {
            restTemplate.exchange(
                    baseUrl() + "/groups/" + groupUuid + "/members/" + member.uuid(),
                    HttpMethod.DELETE,
                    authenticatedRequest(Map.of(), outsider.token()),
                    String.class
            );
            fail("Expected outsider to be denied");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            assertEquals("Apenas o criador ou administradores podem remover usuários", body.get("message").asText());
        }
    }

    @Test
    void shouldRejectMemberRemovalWithoutToken() throws Exception {
        TestUser creator = registerAndLogin("Criador sem token");
        TestUser member = registerAndLogin("Membro sem token");

        String groupUuid = createGroup(creator.uuid());
        addMemberToGroup(groupUuid, creator.token(), member.uuid(), "MEMBER");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        assertStatus(
                () -> restTemplate.exchange(
                        baseUrl() + "/groups/" + groupUuid + "/members/" + member.uuid(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(Map.of(), headers),
                        String.class
                ),
                HttpStatus.UNAUTHORIZED
        );
    }

    @Test
    void shouldUpdateGroupWhenRequesterIsCreatorAndRejectWhenNotCreator() throws Exception {
        TestUser creator = registerAndLogin("Criador update");
        TestUser outsider = registerAndLogin("Fora update");
        String groupUuid = createGroup(creator.uuid());

        ResponseEntity<String> okResponse = restTemplate.exchange(
                baseUrl() + "/groups/" + groupUuid,
                HttpMethod.PATCH,
                authenticatedRequest(Map.of("groupName", "Novo Nome"), creator.token()),
                String.class
        );
        assertEquals(HttpStatus.OK, okResponse.getStatusCode());

        assertStatus(
                () -> restTemplate.exchange(
                        baseUrl() + "/groups/" + groupUuid,
                        HttpMethod.PATCH,
                        authenticatedRequest(Map.of("groupName", "Tentativa"), outsider.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @Test
    void shouldListGroupsAndMembers() throws Exception {
        TestUser creator = registerAndLogin("Criador listagem");
        TestUser member = registerAndLogin("Membro listagem");
        String groupUuid = createGroup(creator.uuid());
        addMemberToGroup(groupUuid, creator.token(), creator.uuid(), "MEMBER");
        addMemberToGroup(groupUuid, creator.token(), member.uuid(), "MEMBER");

        ResponseEntity<String> groupsResponse = restTemplate.exchange(
                baseUrl() + "/groups",
                HttpMethod.GET,
                authenticatedRequest(Map.of(), creator.token()),
                String.class
        );
        assertEquals(HttpStatus.OK, groupsResponse.getStatusCode());
        JsonNode groupsBody = objectMapper.readTree(groupsResponse.getBody());
        assertTrue(groupsBody.isArray());

        ResponseEntity<String> membersResponse = restTemplate.exchange(
                baseUrl() + "/groups/" + groupUuid + "/members",
                HttpMethod.GET,
                authenticatedRequest(Map.of(), creator.token()),
                String.class
        );
        assertEquals(HttpStatus.OK, membersResponse.getStatusCode());
        JsonNode membersBody = objectMapper.readTree(membersResponse.getBody());
        assertTrue(membersBody.isArray());
        assertTrue(membersBody.size() >= 2);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String createGroup(String creatorUuid) {
        Map<String, Object> request = Map.of(
                "groupName", "Grupo " + sequence,
                "creatorUuid", creatorUuid
        );
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/groups", request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        try {
            return objectMapper.readTree(response.getBody()).get("groupUuid").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse create group response", e);
        }
    }

    private void addMemberToGroup(String groupUuid, String token, String userUuid, String role) {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/groups/" + groupUuid + "/members",
                HttpMethod.POST,
                authenticatedRequest(Map.of("userUuid", userUuid, "role", role), token),
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void assertStatus(org.junit.jupiter.api.function.Executable executable, HttpStatus status) {
        try {
            executable.execute();
            fail("Era esperado erro HTTP " + status);
        } catch (HttpClientErrorException ex) {
            assertEquals(status, ex.getStatusCode());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private HttpEntity<Map<String, Object>> authenticatedRequest(Map<String, Object> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private TestUser registerAndLogin(String name) {
        String cpf = String.format("%011d", sequence++);
        RegisterRequestDTO request = new RegisterRequestDTO(name, cpf, "123456", false);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/auth/register", request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            String userUuid = body.get("userUuid").asText();

            ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                    baseUrl() + "/auth/login",
                    Map.of("cpf", cpf, "password", "123456"),
                    String.class
            );
            assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
            String token = objectMapper.readTree(loginResponse.getBody()).get("token").asText();
            return new TestUser(userUuid, token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register/login user", e);
        }
    }

    private String registerUser(String name) {
        return registerAndLogin(name).uuid();
    }

    private record TestUser(String uuid, String token) {
    }
}
