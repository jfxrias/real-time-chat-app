package com.jfxrias.realtimechatappbackend.controller;

import com.jfxrias.realtimechatappbackend.dto.SendGroupMessageRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.EditMessageRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.SendPrivateMessageRequestDTO;
import com.jfxrias.realtimechatappbackend.entity.Message;
import com.jfxrias.realtimechatappbackend.service.MessageService;
import com.jfxrias.realtimechatappbackend.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {
    private final MessageService messageService;
    private final JwtUtil jwtUtil;

    public MessageController(MessageService messageService, JwtUtil jwtUtil) {
        this.messageService = messageService;
        this.jwtUtil = jwtUtil;
    }

    @PatchMapping("/{messageUuid}")
    public ResponseEntity<?> editMessage(
            @PathVariable String messageUuid,
            @Valid @RequestBody EditMessageRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            Message message = messageService.editMessage(authenticatedUserUuid, messageUuid, request.content());
            return ResponseEntity.ok(Map.of(
                    "messageUuid", message.getMessageUuid(),
                    "content", message.getContent(),
                    "editedAt", message.getEditedAt()
            ));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{messageUuid}/delete-for-me")
    public ResponseEntity<?> deleteForMe(
            @PathVariable String messageUuid,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            messageService.deleteMessageForUser(authenticatedUserUuid, messageUuid);
            return ResponseEntity.ok(Map.of("message", "Mensagem marcada como excluída para o usuário"));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/private")
    public ResponseEntity<?> sendPrivate(
            @Valid @RequestBody SendPrivateMessageRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String senderUuid = extractAuthenticatedUserUuid(authorization);
            Message message = messageService.sendPrivateMessage(senderUuid, request.receiverUuid(), request.content());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("messageUuid", message.getMessageUuid());
            response.put("senderUuid", message.getSenderUuid());
            response.put("receiverUuid", request.receiverUuid().trim());
            response.put("content", message.getContent());
            response.put("sentAt", message.getSentAt());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/groups")
    public ResponseEntity<?> sendToGroups(
            @Valid @RequestBody SendGroupMessageRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String senderUuid = extractAuthenticatedUserUuid(authorization);
            List<Message> sentMessages = messageService.sendGroupMessages(senderUuid, request.groupUuids(), request.content());

            List<String> uniqueGroupUuids = request.groupUuids().stream()
                    .map(String::trim)
                    .distinct()
                    .toList();
            List<Map<String, Object>> deliveries = sentMessages.stream().map(message -> {
                Map<String, Object> delivery = new LinkedHashMap<String, Object>();
                int index = sentMessages.indexOf(message);
                String groupUuid = uniqueGroupUuids.get(index);
                delivery.put("messageUuid", message.getMessageUuid());
                delivery.put("senderUuid", message.getSenderUuid());
                delivery.put("targetGroupUuid", groupUuid);
                delivery.put("content", message.getContent());
                delivery.put("sentAt", message.getSentAt());
                return delivery;
            }).toList();

            return ResponseEntity.ok(Map.of(
                    "message", "Mensagem enviada aos grupos",
                    "deliveries", deliveries
            ));
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/private/{otherUserUuid}")
    public ResponseEntity<?> getPrivateMessages(
            @PathVariable String otherUserUuid,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            List<Message> messages = messageService.getPrivateMessages(authenticatedUserUuid, otherUserUuid);
            return ResponseEntity.ok(messages.stream().map(message -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("messageUuid", message.getMessageUuid());
                item.put("senderUuid", message.getSenderUuid());
                item.put("content", message.getContent());
                item.put("sentAt", message.getSentAt());
                item.put("editedAt", message.getEditedAt());
                return item;
            }).toList());
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/groups/{groupUuid}")
    public ResponseEntity<?> getGroupMessages(
            @PathVariable String groupUuid,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            String authenticatedUserUuid = extractAuthenticatedUserUuid(authorization);
            List<Message> messages = messageService.getGroupMessages(authenticatedUserUuid, groupUuid);
            return ResponseEntity.ok(messages.stream().map(message -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("messageUuid", message.getMessageUuid());
                item.put("senderUuid", message.getSenderUuid());
                item.put("content", message.getContent());
                item.put("sentAt", message.getSentAt());
                item.put("editedAt", message.getEditedAt());
                return item;
            }).toList());
        } catch (IllegalArgumentException e) {
            boolean tokenError = e.getMessage().equals("Token inválido ou expirado")
                    || e.getMessage().equals("Token não informado");
            return tokenError
                    ? ResponseEntity.status(401).body(Map.of("message", e.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private String extractAuthenticatedUserUuid(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token não informado");
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            throw new IllegalArgumentException("Token não informado");
        }
        return jwtUtil.extractUserUuid(token);
    }
}
