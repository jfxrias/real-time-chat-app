package com.jfxrias.realtimechatappbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketMessagingIntegrationTest {

    private static int sequence = 7000;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldConnectAndReceivePrivateAndGroupMessagesInRealtime() throws Exception {
        TestUser sender = registerAndLogin("Sender");
        TestUser receiver = registerAndLogin("Receiver");
        String groupUuid = createGroup(sender, "Realtime Group");
        addMemberToGroup(groupUuid, sender, receiver.uuid());

        StompSession session = connectStomp(sender.token());
        BlockingQueue<Map<String, Object>> privateEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Map<String, Object>> groupEvents = new LinkedBlockingQueue<>();

        session.subscribe("/topic/private/" + receiver.uuid(), mapFrameHandler(privateEvents));
        session.subscribe("/topic/groups/" + groupUuid, mapFrameHandler(groupEvents));

        ResponseEntity<String> privateResponse = restTemplate.exchange(
                baseUrl() + "/messages/private",
                HttpMethod.POST,
                authenticatedRequest(Map.of("receiverUuid", receiver.uuid(), "content", "ping-private"), sender.token()),
                String.class
        );
        assertEquals(200, privateResponse.getStatusCode().value());

        ResponseEntity<String> groupResponse = restTemplate.exchange(
                baseUrl() + "/messages/groups",
                HttpMethod.POST,
                authenticatedRequest(Map.of("groupUuids", List.of(groupUuid), "content", "ping-group"), sender.token()),
                String.class
        );
        assertEquals(200, groupResponse.getStatusCode().value());

        Map<String, Object> privateEvent = privateEvents.poll(10, TimeUnit.SECONDS);
        assertNotNull(privateEvent);
        assertEquals(sender.uuid(), privateEvent.get("senderUuid"));
        assertEquals(receiver.uuid(), privateEvent.get("receiverUuid"));
        assertEquals("ping-private", privateEvent.get("content"));

        Map<String, Object> groupEvent = groupEvents.poll(10, TimeUnit.SECONDS);
        assertNotNull(groupEvent);
        assertEquals(sender.uuid(), groupEvent.get("senderUuid"));
        assertEquals(groupUuid, groupEvent.get("targetGroupUuid"));
        assertEquals("ping-group", groupEvent.get("content"));

        session.disconnect();
    }

    @Test
    void shouldRejectStompConnectWithoutOrWithInvalidToken() throws Exception {
        CompletableFuture<Throwable> missingTokenFailure = connectStompExpectingFailure(null);
        Throwable missing = missingTokenFailure.get(10, TimeUnit.SECONDS);
        assertNotNull(missing);

        CompletableFuture<Throwable> invalidTokenFailure = connectStompExpectingFailure("invalid-token");
        Throwable invalid = invalidTokenFailure.get(10, TimeUnit.SECONDS);
        assertNotNull(invalid);
        assertTrue(invalid.getMessage() == null || !invalid.getMessage().isBlank());
    }

    private CompletableFuture<Throwable> connectStompExpectingFailure(String token) {
        WebSocketStompClient stompClient = createStompClient();
        CompletableFuture<Throwable> future = new CompletableFuture<>();
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }

        stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        future.complete(exception);
                    }
                }
        ).whenComplete((session, throwable) -> {
            if (throwable != null) {
                future.complete(throwable);
            } else {
                session.disconnect();
                future.complete(new IllegalStateException("Conexão STOMP não deveria ser aceita sem token válido"));
            }
        });
        return future;
    }

    private StompSession connectStomp(String token) throws Exception {
        WebSocketStompClient stompClient = createStompClient();
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token);
        return stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {
                }
        ).get(10, TimeUnit.SECONDS);
    }

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        return client;
    }

    private StompFrameHandler mapFrameHandler(BlockingQueue<Map<String, Object>> queue) {
        return new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                queue.offer((Map<String, Object>) payload);
            }
        };
    }

    private TestUser registerAndLogin(String namePrefix) throws Exception {
        String cpf = uniqueCpf();
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                Map.of("name", namePrefix + uniqueSuffix(), "cpf", cpf, "password", "123456", "isAdmin", false),
                String.class
        );
        JsonNode registerBody = objectMapper.readTree(registerResponse.getBody());
        String userUuid = registerBody.get("userUuid").asText();

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                Map.of("cpf", cpf, "password", "123456"),
                String.class
        );
        JsonNode loginBody = objectMapper.readTree(loginResponse.getBody());
        String token = loginBody.get("token").asText();

        return new TestUser(userUuid, token);
    }

    private String createGroup(TestUser creator, String name) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/groups",
                HttpMethod.POST,
                authenticatedRequest(Map.of("groupName", name + uniqueSuffix(), "creatorUuid", creator.uuid()), creator.token()),
                String.class
        );
        return objectMapper.readTree(response.getBody()).get("groupUuid").asText();
    }

    private void addMemberToGroup(String groupUuid, TestUser requester, String memberUuid) {
        restTemplate.exchange(
                baseUrl() + "/groups/" + groupUuid.trim() + "/members",
                HttpMethod.POST,
                authenticatedRequest(Map.of("userUuid", memberUuid, "role", "MEMBER"), requester.token()),
                String.class
        );
    }

    private HttpEntity<Map<String, Object>> authenticatedRequest(Map<String, Object> body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String uniqueCpf() {
        sequence++;
        return String.format("%011d", sequence);
    }

    private String uniqueSuffix() {
        return "-" + sequence;
    }

    private record TestUser(String uuid, String token) {
    }
}
