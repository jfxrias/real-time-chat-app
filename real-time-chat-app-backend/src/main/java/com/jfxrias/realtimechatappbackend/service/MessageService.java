package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.entity.*;
import com.jfxrias.realtimechatappbackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public MessageService(MessageRepository messageRepository,
                          PrivateMessageRepository privateMessageRepository,
                          GroupMessageRepository groupMessageRepository,
                          UserRepository userRepository,
                          GroupRepository groupRepository,
                          GroupMemberRepository groupMemberRepository) {
        this.messageRepository = messageRepository;
        this.privateMessageRepository = privateMessageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Transactional
    public Message sendPrivateMessage(String senderUuid, String receiverUuid, String content) {
        String normalizedSender = normalizeRequired(senderUuid, "Usuário autenticado é obrigatório");
        String normalizedReceiver = normalizeRequired(receiverUuid, "UUID do destinatário é obrigatório");
        String normalizedContent = normalizeContent(content);

        userRepository.findById(normalizedSender)
                .orElseThrow(() -> new IllegalArgumentException("Usuário remetente não encontrado"));
        userRepository.findById(normalizedReceiver)
                .orElseThrow(() -> new IllegalArgumentException("Usuário destinatário não encontrado"));

        Message message = messageRepository.save(new Message(normalizedSender, normalizedContent));
        privateMessageRepository.save(new PrivateMessage(message.getMessageUuid(), normalizedReceiver));
        return message;
    }

    @Transactional
    public List<Message> sendGroupMessages(String senderUuid, List<String> groupUuids, String content) {
        String normalizedSender = normalizeRequired(senderUuid, "Usuário autenticado é obrigatório");
        String normalizedContent = normalizeContent(content);

        userRepository.findById(normalizedSender)
                .orElseThrow(() -> new IllegalArgumentException("Usuário remetente não encontrado"));
        if (groupUuids == null || groupUuids.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um grupo");
        }

        LinkedHashSet<String> uniqueGroupUuids = new LinkedHashSet<>();
        for (String groupUuid : groupUuids) {
            String normalized = normalizeRequired(groupUuid, "UUID do grupo é obrigatório");
            uniqueGroupUuids.add(normalized);
        }

        List<Message> sentMessages = new ArrayList<>();
        for (String groupUuid : uniqueGroupUuids) {
            Group group = groupRepository.findById(groupUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado: " + groupUuid));
            if (!"ACTIVE".equalsIgnoreCase(group.getStatus())) {
                throw new IllegalArgumentException("O grupo não está ativo: " + groupUuid);
            }
            if (!groupMemberRepository.existsById(new GroupMemberId(groupUuid, normalizedSender))) {
                throw new IllegalArgumentException("Usuário não participa do grupo: " + groupUuid);
            }

            Message message = messageRepository.save(new Message(normalizedSender, normalizedContent));
            groupMessageRepository.save(new GroupMessage(message.getMessageUuid(), groupUuid));
            sentMessages.add(message);
        }
        return sentMessages;
    }

    @Transactional
    public Message editMessage(String authenticatedUserUuid, String messageUuid, String content) {
        String normalizedUserUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        String normalizedMessageUuid = normalizeRequired(messageUuid, "UUID da mensagem é obrigatório");
        String normalizedContent = normalizeContent(content);

        Message message = messageRepository.findById(normalizedMessageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));

        if (!normalizedUserUuid.equals(message.getSenderUuid())) {
            throw new IllegalArgumentException("Usuário não pode editar esta mensagem");
        }

        if (!privateMessageRepository.existsById(normalizedMessageUuid)
                && !groupMessageRepository.existsById(normalizedMessageUuid)) {
            throw new IllegalArgumentException("Mensagem não está vinculada a conversa privada ou grupo");
        }

        message.setContent(normalizedContent);
        message.setEditedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessageForUser(String authenticatedUserUuid, String messageUuid) {
        String normalizedUserUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        String normalizedMessageUuid = normalizeRequired(messageUuid, "UUID da mensagem é obrigatório");

        Message message = messageRepository.findById(normalizedMessageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));

        if (!normalizedUserUuid.equals(message.getSenderUuid())) {
            throw new IllegalArgumentException("Usuário não pode excluir esta mensagem para si");
        }

        if (!privateMessageRepository.existsById(normalizedMessageUuid)
                && !groupMessageRepository.existsById(normalizedMessageUuid)) {
            throw new IllegalArgumentException("Mensagem não está vinculada a conversa privada ou grupo");
        }

        message.setIsDeletedBySender(true);
        messageRepository.save(message);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? null : content.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Conteúdo da mensagem é obrigatório");
        }
        return normalized;
    }
}
