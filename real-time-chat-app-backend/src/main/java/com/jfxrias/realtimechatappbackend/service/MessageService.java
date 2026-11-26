package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.entity.*;
import com.jfxrias.realtimechatappbackend.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          PrivateMessageRepository privateMessageRepository,
                          GroupMessageRepository groupMessageRepository,
                          UserRepository userRepository,
                          GroupRepository groupRepository,
                          GroupMemberRepository groupMemberRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.privateMessageRepository = privateMessageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.messagingTemplate = messagingTemplate;
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
        PrivateRealtimeMessageDTO privateDto = new PrivateRealtimeMessageDTO(
                message.getMessageUuid(),
                message.getSenderUuid(),
                normalizedReceiver,
                message.getContent(),
                message.getSentAt()
        );
        messagingTemplate.convertAndSend("/topic/private/" + normalizedReceiver, privateDto);
        messagingTemplate.convertAndSend("/topic/private/" + normalizedSender, privateDto);
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
            GroupRealtimeMessageDTO groupDto = new GroupRealtimeMessageDTO(
                    message.getMessageUuid(),
                    message.getSenderUuid(),
                    groupUuid,
                    message.getContent(),
                    message.getSentAt()
            );
            messagingTemplate.convertAndSend("/topic/groups/" + groupUuid, groupDto);
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

        Optional<PrivateMessage> privateMessage = privateMessageRepository.findById(normalizedMessageUuid);
        Optional<GroupMessage> groupMessage = groupMessageRepository.findById(normalizedMessageUuid);
        if (privateMessage.isEmpty() && groupMessage.isEmpty()) {
            throw new IllegalArgumentException("Mensagem não está vinculada a conversa privada ou grupo");
        }

        message.setContent(normalizedContent);
        message.setEditedAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        if (privateMessage.isPresent()) {
            String receiverUuid = privateMessage.get().getReceiverUuid();
            PrivateMessageEditedDTO editedDto = new PrivateMessageEditedDTO(
                    saved.getMessageUuid(),
                    saved.getSenderUuid(),
                    receiverUuid,
                    saved.getContent(),
                    saved.getSentAt(),
                    saved.getEditedAt()
            );
            messagingTemplate.convertAndSend("/topic/private/" + receiverUuid, editedDto);
            messagingTemplate.convertAndSend("/topic/private/" + saved.getSenderUuid(), editedDto);
        } else {
            String targetGroupUuid = groupMessage.orElseThrow().getTargetGroupUuid();
            GroupMessageEditedDTO editedDto = new GroupMessageEditedDTO(
                    saved.getMessageUuid(),
                    saved.getSenderUuid(),
                    targetGroupUuid,
                    saved.getContent(),
                    saved.getSentAt(),
                    saved.getEditedAt()
            );
            messagingTemplate.convertAndSend("/topic/groups/" + targetGroupUuid, editedDto);
        }
        return saved;
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

    public List<Message> getPrivateMessages(String authenticatedUserUuid, String otherUserUuid) {
        String normalizedAuthenticatedUser = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        String normalizedOtherUser = normalizeRequired(otherUserUuid, "UUID do usuário é obrigatório");

        userRepository.findById(normalizedAuthenticatedUser)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado"));
        userRepository.findById(normalizedOtherUser)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        List<String> messageUuids = privateMessageRepository.findConversationMessageUuids(
                normalizedAuthenticatedUser,
                normalizedOtherUser
        );

        return messageRepository.findAllById(messageUuids).stream()
                .filter(message -> !(normalizedAuthenticatedUser.equals(message.getSenderUuid())
                        && Boolean.TRUE.equals(message.getIsDeletedBySender())))
                .sorted(Comparator.comparing(Message::getSentAt))
                .toList();
    }

    public List<Message> getGroupMessages(String authenticatedUserUuid, String groupUuid) {
        String normalizedAuthenticatedUser = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        String normalizedGroupUuid = normalizeRequired(groupUuid, "UUID do grupo é obrigatório");

        userRepository.findById(normalizedAuthenticatedUser)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado"));

        groupRepository.findById(normalizedGroupUuid)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));

        if (!groupMemberRepository.existsById(new GroupMemberId(normalizedGroupUuid, normalizedAuthenticatedUser))) {
            throw new IllegalArgumentException("Usuário não participa do grupo");
        }

        List<String> messageUuids = groupMessageRepository.findByTargetGroupUuid(normalizedGroupUuid).stream()
                .map(GroupMessage::getMessageUuid)
                .toList();

        return messageRepository.findAllById(messageUuids).stream()
                .filter(message -> !(normalizedAuthenticatedUser.equals(message.getSenderUuid())
                        && Boolean.TRUE.equals(message.getIsDeletedBySender())))
                .sorted(Comparator.comparing(Message::getSentAt))
                .toList();
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

    private record PrivateRealtimeMessageDTO(
            String messageUuid,
            String senderUuid,
            String receiverUuid,
            String content,
            LocalDateTime sentAt
    ) {
    }

    private record GroupRealtimeMessageDTO(
            String messageUuid,
            String senderUuid,
            String targetGroupUuid,
            String content,
            LocalDateTime sentAt
    ) {
    }

    private record PrivateMessageEditedDTO(
            String messageUuid,
            String senderUuid,
            String receiverUuid,
            String content,
            LocalDateTime sentAt,
            LocalDateTime editedAt
    ) {
    }

    private record GroupMessageEditedDTO(
            String messageUuid,
            String senderUuid,
            String targetGroupUuid,
            String content,
            LocalDateTime sentAt,
            LocalDateTime editedAt
    ) {
    }
}
