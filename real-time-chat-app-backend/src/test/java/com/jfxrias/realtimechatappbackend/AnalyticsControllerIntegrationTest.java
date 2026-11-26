package com.jfxrias.realtimechatappbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfxrias.realtimechatappbackend.dto.RegisterRequestDTO;
import com.jfxrias.realtimechatappbackend.entity.*;
import com.jfxrias.realtimechatappbackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AnalyticsControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PrivateMessageRepository privateMessageRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;
    private User ana;
    private User bruno;
    private User carla;
    private User diego;
    private Group groupOne;
    private Group groupTwo;
    private Group groupThree;

    @BeforeEach
    void setup() throws Exception {
        reportRepository.deleteAll();
        groupMessageRepository.deleteAll();
        privateMessageRepository.deleteAll();
        messageRepository.deleteAll();
        friendshipRepository.deleteAll();
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        ana = registerAndSyncCreatedAt("11111111111", "Ana", true, LocalDateTime.of(2026, 1, 1, 10, 0));
        bruno = registerAndSyncCreatedAt("22222222222", "Bruno", false, LocalDateTime.of(2026, 1, 2, 10, 0));
        carla = registerAndSyncCreatedAt("33333333333", "Carla", false, LocalDateTime.of(2026, 1, 3, 10, 0));
        diego = registerAndSyncCreatedAt("44444444444", "Diego", false, LocalDateTime.of(2025, 1, 1, 10, 0));

        groupOne = createGroup("Group One", ana.getUserUuid(), LocalDateTime.of(2026, 1, 10, 10, 0));
        groupTwo = createGroup("Group Two", bruno.getUserUuid(), LocalDateTime.of(2026, 1, 11, 10, 0));
        groupThree = createGroup("Group Three", carla.getUserUuid(), LocalDateTime.of(2026, 1, 12, 10, 0));

        createGroupMember(groupOne.getGroupUuid(), ana.getUserUuid(), "ADMIN");
        createGroupMember(groupOne.getGroupUuid(), bruno.getUserUuid(), "MEMBER");
        createGroupMember(groupOne.getGroupUuid(), carla.getUserUuid(), "MEMBER");

        createGroupMember(groupTwo.getGroupUuid(), ana.getUserUuid(), "MEMBER");
        createGroupMember(groupTwo.getGroupUuid(), bruno.getUserUuid(), "ADMIN");

        createGroupMember(groupThree.getGroupUuid(), ana.getUserUuid(), "MEMBER");

        friendshipRepository.save(new Friendship(ana.getUserUuid(), bruno.getUserUuid()));
        friendshipRepository.save(new Friendship(carla.getUserUuid(), ana.getUserUuid()));
        friendshipRepository.save(new Friendship(bruno.getUserUuid(), carla.getUserUuid()));

        Message p1 = createMessage(ana.getUserUuid(), "private 1", LocalDateTime.of(2026, 1, 15, 10, 0));
        privateMessageRepository.save(new PrivateMessage(p1.getMessageUuid(), bruno.getUserUuid()));

        Message p2 = createMessage(carla.getUserUuid(), "private 2", LocalDateTime.of(2026, 1, 16, 11, 0));
        privateMessageRepository.save(new PrivateMessage(p2.getMessageUuid(), bruno.getUserUuid()));

        Message p3 = createMessage(bruno.getUserUuid(), "private 3", LocalDateTime.of(2026, 2, 1, 10, 0));
        privateMessageRepository.save(new PrivateMessage(p3.getMessageUuid(), ana.getUserUuid()));

        Message g1 = createMessage(bruno.getUserUuid(), "group one msg", LocalDateTime.of(2026, 1, 20, 12, 0));
        groupMessageRepository.save(new GroupMessage(g1.getMessageUuid(), groupOne.getGroupUuid()));

        Message g2 = createMessage(ana.getUserUuid(), "group one msg 2", LocalDateTime.of(2026, 1, 21, 12, 0));
        groupMessageRepository.save(new GroupMessage(g2.getMessageUuid(), groupOne.getGroupUuid()));

        Message g3 = createMessage(bruno.getUserUuid(), "group two msg", LocalDateTime.of(2026, 1, 22, 12, 0));
        groupMessageRepository.save(new GroupMessage(g3.getMessageUuid(), groupTwo.getGroupUuid()));

        reportRepository.save(createReport(ana.getUserUuid(), bruno.getUserUuid(), groupOne.getGroupUuid(), "Spam", LocalDateTime.of(2026, 2, 10, 10, 0)));
        reportRepository.save(createReport(carla.getUserUuid(), bruno.getUserUuid(), groupOne.getGroupUuid(), "Spam", LocalDateTime.of(2026, 2, 11, 10, 0)));
        reportRepository.save(createReport(diego.getUserUuid(), bruno.getUserUuid(), groupTwo.getGroupUuid(), "Abuso", LocalDateTime.of(2026, 2, 12, 10, 0)));
        reportRepository.save(createReport(ana.getUserUuid(), bruno.getUserUuid(), groupTwo.getGroupUuid(), "Spam", LocalDateTime.of(2026, 2, 13, 10, 0)));

        token = authenticateAndGetToken(ana.getCpf(), "123456");
    }

    @Test
    void shouldReturn401WithoutToken() {
        assertStatus(
                () -> restTemplate.getForEntity(url("/analytics/messages/received-percentage"), String.class),
                HttpStatus.UNAUTHORIZED
        );
    }

    @Test
    void shouldReturnReceivedPercentage() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url("/analytics/messages/received-percentage"), HttpMethod.GET, authorized(), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(4, body.size());
        assertEquals("Bruno", body.get(0).get("name").asText());
        assertEquals(4, body.get(0).get("receivedCount").asInt());
    }

    @Test
    void shouldReturnUsersWithMostFriends() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url("/analytics/users/most-friends"), HttpMethod.GET, authorized(), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Ana", body.get(0).get("name").asText());
        assertEquals(2, body.get(0).get("totalFriends").asInt());
    }

    @Test
    void shouldReturnGroupMemberCount() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url("/analytics/groups/member-count"), HttpMethod.GET, authorized(), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Group One", body.get(0).get("groupName").asText());
        assertEquals(3, body.get(0).get("totalUsers").asInt());
    }

    @Test
    void shouldReturnTopReportedUsers() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url("/analytics/reports/top-reported"), HttpMethod.GET, authorized(), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(1, body.size());
        assertEquals("Bruno", body.get(0).get("name").asText());
        assertEquals(4, body.get(0).get("totalReports").asInt());
    }

    @Test
    void shouldReturnCommonReasons() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url("/analytics/reports/common-reasons"), HttpMethod.GET, authorized(), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Spam", body.get(0).get("reason").asText());
        assertEquals(3, body.get(0).get("totalReports").asInt());
    }

    @Test
    void shouldReturnUsersWithMostGroups() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url("/analytics/users/most-groups"), HttpMethod.GET, authorized(), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Ana", body.get(0).get("name").asText());
        assertEquals(3, body.get(0).get("totalGroups").asInt());
    }

    @Test
    void shouldReturnMostInactiveUser() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url("/analytics/users/most-inactive"), HttpMethod.GET, authorized(), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Diego", body.get("name").asText());
        assertTrue(body.has("lastMessageAt"));
        assertTrue(body.has("createdAt"));
    }

    @Test
    void shouldReturnUserMessageStatsInPeriod() throws Exception {
        String start = "2026-01-01T00:00:00";
        String end = "2026-01-31T23:59:59";
        ResponseEntity<String> response = restTemplate.exchange(
                url("/analytics/users/" + bruno.getUserUuid() + "/message-stats?start=" + start + "&end=" + end),
                HttpMethod.GET,
                authorized(),
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Bruno", body.get("name").asText());
        assertEquals(2, body.get("sentCount").asInt());
        assertEquals(4, body.get("receivedCount").asInt());
        assertEquals(6, body.get("totalCount").asInt());
    }

    @Test
    void shouldReturn400WhenStartAfterEnd() {
        String start = "2026-02-01T00:00:00";
        String end = "2026-01-01T00:00:00";
        try {
            restTemplate.exchange(
                    url("/analytics/users/" + bruno.getUserUuid() + "/message-stats?start=" + start + "&end=" + end),
                    HttpMethod.GET,
                    authorized(),
                    String.class
            );
            fail("Expected bad request");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getResponseBodyAsString().contains("start nao pode ser posterior a end"));
        }
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

    private User registerAndSyncCreatedAt(String cpf, String name, boolean admin, LocalDateTime createdAt) {
        RegisterRequestDTO request = new RegisterRequestDTO(name, cpf, "123456", admin);
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(url("/auth/register"), request, String.class);
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());

        User user = userRepository.findByCpf(cpf).orElseThrow(() -> new IllegalStateException("User not found after register"));
        jdbcTemplate.update("UPDATE `user` SET created_at = ? WHERE user_uuid = ?", Timestamp.valueOf(createdAt), user.getUserUuid());
        return userRepository.findById(user.getUserUuid()).orElseThrow(() -> new IllegalStateException("User not found after createdAt sync"));
    }

    private Group createGroup(String groupName, String creatorUuid, LocalDateTime createdAt) {
        Group group = new Group();
        group.setGroupName(groupName);
        group.setStatus("ACTIVE");
        group.setCreatorUuid(creatorUuid);
        group.setCreatedAt(createdAt);
        return groupRepository.save(group);
    }

    private void createGroupMember(String groupUuid, String userUuid, String role) {
        GroupMember member = new GroupMember(groupUuid, userUuid, role);
        groupMemberRepository.save(member);
    }

    private Message createMessage(String senderUuid, String content, LocalDateTime sentAt) {
        Message message = new Message();
        message.setSenderUuid(senderUuid);
        message.setContent(content);
        message.setSentAt(sentAt);
        message.setIsDeletedBySender(false);
        return messageRepository.save(message);
    }

    private Report createReport(String reporterUuid, String reportedUuid, String groupUuid, String reason, LocalDateTime reportedAt) {
        Report report = new Report();
        report.setReporterUuid(reporterUuid);
        report.setReportedUuid(reportedUuid);
        report.setGroupUuid(groupUuid);
        report.setReason(reason);
        report.setReportedAt(reportedAt);
        return report;
    }

    private String authenticateAndGetToken(String cpf, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("cpf", cpf, "password", password), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url("/auth/login"), request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());
        return body.get("token").asText();
    }

    private HttpEntity<Void> authorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
