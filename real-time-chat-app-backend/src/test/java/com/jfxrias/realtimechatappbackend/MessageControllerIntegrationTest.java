package com.jfxrias.realtimechatappbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfxrias.realtimechatappbackend.repository.GroupMessageRepository;
import com.jfxrias.realtimechatappbackend.repository.MessageRepository;
import com.jfxrias.realtimechatappbackend.repository.PrivateMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessageControllerIntegrationTest {
    private static int sequence = 3000;

    @LocalServerPort
    private int port;

    @Autowired
    private PrivateMessageRepository privateMessageRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private MessageRepository messageRepository;

    private final RestTemplate restTemplate = patchEnabledRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSendPrivateMessageToSelfAndPersistIt() throws Exception {
        TestUser user = registerAndLogin("Usuário privado");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/messages/private",
                HttpMethod.POST,
                authenticatedRequest(Map.of("receiverUuid", user.uuid(), "content", "Mensagem para mim"), user.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(user.uuid(), body.get("senderUuid").asText());
        assertEquals(user.uuid(), body.get("receiverUuid").asText());
        assertEquals("Mensagem para mim", body.get("content").asText());
        assertTrue(privateMessageRepository.count() > 0);
    }

    @Test
    void shouldRejectPrivateMessageWithoutToken() {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(
                "receiverUuid", "uuid-qualquer",
                "content", "Olá"
        ), new HttpHeaders());

        assertStatus(
                () -> restTemplate.exchange(baseUrl() + "/messages/private", HttpMethod.POST, request, String.class),
                HttpStatus.UNAUTHORIZED
        );
    }

    @Test
    void shouldRejectPrivateMessageForUnknownReceiver() {
        TestUser sender = registerAndLogin("Remetente privado");

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/messages/private",
                        HttpMethod.POST,
                        authenticatedRequest(Map.of("receiverUuid", "usuario-inexistente", "content", "Olá"), sender.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Usuário destinatário não encontrado"
        );
    }

    @Test
    void shouldCreateReportWhenAuthenticatedAndReportedUserExists() throws Exception {
        TestUser reporter = registerAndLogin("Usuário denunciante");
        TestUser reported = registerAndLogin("Usuário denunciado");
        String groupUuid = createGroup(reporter, "Grupo da denúncia");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/reports",
                HttpMethod.POST,
                authenticatedRequest(Map.of(
                        "reportedUuid", reported.uuid(),
                        "groupUuid", groupUuid,
                        "reason", "Comportamento inadequado"
                ), reporter.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(reporter.uuid(), body.get("reporterUuid").asText());
        assertEquals(reported.uuid(), body.get("reportedUuid").asText());
        assertEquals(groupUuid, body.get("groupUuid").asText());
        assertEquals("Comportamento inadequado", body.get("reason").asText());
        assertTrue(body.get("reportUuid").asText().length() > 0);
    }

    @Test
    void shouldRejectReportWithoutToken() {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(
                "reportedUuid", "uuid-qualquer",
                "groupUuid", "uuid-grupo",
                "reason", "Motivo"
        ), new HttpHeaders());

        assertStatus(
                () -> restTemplate.exchange(baseUrl() + "/reports", HttpMethod.POST, request, String.class),
                HttpStatus.UNAUTHORIZED
        );
    }

    @Test
    void shouldRejectReportForUnknownReportedUser() {
        TestUser reporter = registerAndLogin("Denunciante desconhecido");
        String groupUuid = createGroup(reporter, "Grupo denúncia inexistente");

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/reports",
                        HttpMethod.POST,
                        authenticatedRequest(Map.of(
                                "reportedUuid", "usuario-inexistente",
                                "groupUuid", groupUuid,
                                "reason", "Motivo"
                        ), reporter.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Usuário reportado não encontrado"
        );
    }

    @Test
    void shouldSendOneMessageToTwoGroupsAndAvoidDuplicateGroupUuid() throws Exception {
        TestUser sender = registerAndLogin("Remetente grupo");
        String firstGroup = createGroup(sender, "Grupo um");
        String secondGroup = createGroup(sender, "Grupo dois");

        Map<String, Object> body = Map.of(
                "groupUuids", List.of(firstGroup, secondGroup, firstGroup),
                "content", "Aviso único"
        );

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/messages/groups",
                HttpMethod.POST,
                authenticatedRequest(body, sender.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("Mensagem enviada aos grupos", responseBody.get("message").asText());
        assertEquals(2, responseBody.get("deliveries").size());
        assertTrue(groupMessageRepository.count() >= 2);
    }

    @Test
    void shouldRejectMessageWhenUserDoesNotParticipateInGroup() {
        TestUser creator = registerAndLogin("Criador grupo");
        TestUser outsider = registerAndLogin("Usuário externo");
        String groupUuid = createGroup(creator, "Grupo protegido");

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/messages/groups",
                        HttpMethod.POST,
                        authenticatedRequest(Map.of("groupUuids", List.of(groupUuid), "content", "Não autorizado"), outsider.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Usuário não participa do grupo: " + groupUuid
        );
    }

    @Test
    void shouldRejectUnknownGroup() {
        TestUser sender = registerAndLogin("Remetente desconhecido");

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/messages/groups",
                        HttpMethod.POST,
                        authenticatedRequest(Map.of("groupUuids", List.of("grupo-inexistente"), "content", "Olá"), sender.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Grupo não encontrado: grupo-inexistente"
        );
    }

    @Test
    void shouldRejectEmptyGroupList() {
        TestUser sender = registerAndLogin("Remetente sem grupos");

        assertStatus(
                () -> restTemplate.exchange(
                        baseUrl() + "/messages/groups",
                        HttpMethod.POST,
                        authenticatedRequest(Map.of("groupUuids", List.of(), "content", "Olá"), sender.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @Test
    void shouldRejectEmptyPrivateContent() {
        TestUser sender = registerAndLogin("Remetente conteúdo vazio");

        assertStatus(
                () -> restTemplate.exchange(
                        baseUrl() + "/messages/private",
                        HttpMethod.POST,
                        authenticatedRequest(Map.of("receiverUuid", sender.uuid(), "content", "   "), sender.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @Test
    void shouldEditPrivateMessageWhenAuthenticatedSender() throws Exception {
        TestUser user = registerAndLogin("Editor privado");

        ResponseEntity<String> sentResponse = restTemplate.exchange(
                baseUrl() + "/messages/private",
                HttpMethod.POST,
                authenticatedRequest(Map.of("receiverUuid", user.uuid(), "content", "Texto antigo"), user.token()),
                String.class
        );
        JsonNode sentBody = objectMapper.readTree(sentResponse.getBody());
        String messageUuid = sentBody.get("messageUuid").asText();

        ResponseEntity<String> editResponse = restTemplate.exchange(
                baseUrl() + "/messages/" + messageUuid,
                HttpMethod.PATCH,
                authenticatedRequest(Map.of("content", "Texto editado"), user.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, editResponse.getStatusCode());
        JsonNode editBody = objectMapper.readTree(editResponse.getBody());
        assertEquals(messageUuid, editBody.get("messageUuid").asText());
        assertEquals("Texto editado", editBody.get("content").asText());
        assertNotNull(messageRepository.findById(messageUuid).orElseThrow().getEditedAt());
    }

    @Test
    void shouldEditGroupMessageWhenAuthenticatedSender() throws Exception {
        TestUser sender = registerAndLogin("Editor grupo");
        String groupUuid = createGroup(sender, "Grupo edição");

        ResponseEntity<String> sendResponse = restTemplate.exchange(
                baseUrl() + "/messages/groups",
                HttpMethod.POST,
                authenticatedRequest(Map.of("groupUuids", List.of(groupUuid), "content", "Aviso antigo"), sender.token()),
                String.class
        );
        JsonNode sendBody = objectMapper.readTree(sendResponse.getBody());
        String messageUuid = sendBody.get("deliveries").get(0).get("messageUuid").asText();

        ResponseEntity<String> editResponse = restTemplate.exchange(
                baseUrl() + "/messages/" + messageUuid,
                HttpMethod.PATCH,
                authenticatedRequest(Map.of("content", "Aviso novo"), sender.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, editResponse.getStatusCode());
        JsonNode editBody = objectMapper.readTree(editResponse.getBody());
        assertEquals("Aviso novo", editBody.get("content").asText());
    }

    @Test
    void shouldDeletePrivateMessageForSender() throws Exception {
        TestUser sender = registerAndLogin("Remetente exclusão");
        TestUser receiver = registerAndLogin("Destino exclusão");

        ResponseEntity<String> sendResponse = restTemplate.exchange(
                baseUrl() + "/messages/private",
                HttpMethod.POST,
                authenticatedRequest(Map.of("receiverUuid", receiver.uuid(), "content", "Apagar para receptor"), sender.token()),
                String.class
        );
        String messageUuid = objectMapper.readTree(sendResponse.getBody()).get("messageUuid").asText();

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl() + "/messages/" + messageUuid + "/delete-for-me",
                HttpMethod.PATCH,
                authenticatedRequest(Map.of(), sender.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertTrue(messageRepository.findById(messageUuid).orElseThrow().getIsDeletedBySender());
    }

    @Test
    void shouldRejectDeletePrivateMessageForReceiver() throws Exception {
        TestUser sender = registerAndLogin("Remetente privado sem permissão");
        TestUser receiver = registerAndLogin("Receptor privado sem permissão");

        ResponseEntity<String> sendResponse = restTemplate.exchange(
                baseUrl() + "/messages/private",
                HttpMethod.POST,
                authenticatedRequest(Map.of("receiverUuid", receiver.uuid(), "content", "Mensagem privada"), sender.token()),
                String.class
        );
        String messageUuid = objectMapper.readTree(sendResponse.getBody()).get("messageUuid").asText();

        assertError(
                () -> restTemplate.exchange(
                        baseUrl() + "/messages/" + messageUuid + "/delete-for-me",
                        HttpMethod.PATCH,
                        authenticatedRequest(Map.of(), receiver.token()),
                        String.class
                ),
                HttpStatus.BAD_REQUEST,
                "Usuário não pode excluir esta mensagem para si"
        );
    }

    @Test
    void shouldDeleteGroupMessageForSender() throws Exception {
        TestUser sender = registerAndLogin("Remetente grupo exclusão");
        String groupUuid = createGroup(sender, "Grupo exclusão");

        ResponseEntity<String> sendResponse = restTemplate.exchange(
                baseUrl() + "/messages/groups",
                HttpMethod.POST,
                authenticatedRequest(Map.of("groupUuids", List.of(groupUuid), "content", "Apagar para mim"), sender.token()),
                String.class
        );
        String messageUuid = objectMapper.readTree(sendResponse.getBody()).get("deliveries").get(0).get("messageUuid").asText();

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                baseUrl() + "/messages/" + messageUuid + "/delete-for-me",
                HttpMethod.PATCH,
                authenticatedRequest(Map.of(), sender.token()),
                String.class
        );

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertTrue(messageRepository.findById(messageUuid).orElseThrow().getIsDeletedBySender());
    }

    @Test
    void shouldHideDeletedPrivateMessageFromSenderOnGetPrivateMessages() throws Exception {
        TestUser sender = registerAndLogin("Sender private list");
        TestUser receiver = registerAndLogin("Receiver private list");

        ResponseEntity<String> sendResponse = restTemplate.exchange(
                baseUrl() + "/messages/private",
                HttpMethod.POST,
                authenticatedRequest(Map.of("receiverUuid", receiver.uuid(), "content", "Oculta privada"), sender.token()),
                String.class
        );
        String messageUuid = objectMapper.readTree(sendResponse.getBody()).get("messageUuid").asText();

        restTemplate.exchange(
                baseUrl() + "/messages/" + messageUuid + "/delete-for-me",
                HttpMethod.PATCH,
                authenticatedRequest(Map.of(), sender.token()),
                String.class
        );

        ResponseEntity<String> listResponse = restTemplate.exchange(
                baseUrl() + "/messages/private/" + receiver.uuid(),
                HttpMethod.GET,
                authenticatedRequest(Map.of(), sender.token()),
                String.class
        );
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        JsonNode listBody = objectMapper.readTree(listResponse.getBody());
        assertTrue(listBody.isArray());
        for (JsonNode n : listBody) {
            assertNotEquals(messageUuid, n.get("messageUuid").asText());
        }
    }

    @Test
    void shouldHideDeletedGroupMessageFromSenderOnGetGroupMessages() throws Exception {
        TestUser sender = registerAndLogin("Sender group list");
        String groupUuid = createGroup(sender, "Grupo listagem");

        ResponseEntity<String> sendResponse = restTemplate.exchange(
                baseUrl() + "/messages/groups",
                HttpMethod.POST,
                authenticatedRequest(Map.of("groupUuids", List.of(groupUuid), "content", "Oculta grupo"), sender.token()),
                String.class
        );
        String messageUuid = objectMapper.readTree(sendResponse.getBody()).get("deliveries").get(0).get("messageUuid").asText();

        restTemplate.exchange(
                baseUrl() + "/messages/" + messageUuid + "/delete-for-me",
                HttpMethod.PATCH,
                authenticatedRequest(Map.of(), sender.token()),
                String.class
        );

        ResponseEntity<String> listResponse = restTemplate.exchange(
                baseUrl() + "/messages/groups/" + groupUuid,
                HttpMethod.GET,
                authenticatedRequest(Map.of(), sender.token()),
                String.class
        );
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        JsonNode listBody = objectMapper.readTree(listResponse.getBody());
        assertTrue(listBody.isArray());
        for (JsonNode n : listBody) {
            assertNotEquals(messageUuid, n.get("messageUuid").asText());
        }
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
            JsonNode body = objectMapper.readTree(response.getBody());
            String groupUuid = body.get("groupUuid").asText();

            // Quando criado pelo usuário, o projeto ainda não adiciona o criador automaticamente.
            // Adiciona-o explicitamente ao grupo para validar o fluxo completo de participação.
            HttpEntity<Map<String, Object>> memberRequest = authenticatedRequest(
                    Map.of("userUuid", creator.uuid(), "role", "MEMBER"),
                    creator.token()
            );
            restTemplate.exchange(
                    baseUrl() + "/groups/" + groupUuid + "/members",
                    HttpMethod.POST,
                    memberRequest,
                    String.class
            );
            return groupUuid;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestUser registerAndLogin(String name) {
        String cpf = String.format("%011d", sequence++);

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                Map.of("name", name, "cpf", cpf, "password", "123456", "isAdmin", false),
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

    private RestTemplate patchEnabledRestTemplate() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }

    private record TestUser(String uuid, String token) {
    }
}
