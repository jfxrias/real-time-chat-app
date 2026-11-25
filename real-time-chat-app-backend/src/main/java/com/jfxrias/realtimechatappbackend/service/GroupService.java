package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.entity.Group;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.repository.GroupMemberRepository;
import com.jfxrias.realtimechatappbackend.repository.GroupRepository;
import com.jfxrias.realtimechatappbackend.repository.UserRepository;
import com.jfxrias.realtimechatappbackend.entity.GroupMember;
import com.jfxrias.realtimechatappbackend.entity.GroupMemberId;
import org.springframework.stereotype.Service;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository,
                        GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public Group createGroup(String groupName, String creatorUuid) {
        String normalizedName = groupName == null ? null : groupName.trim();
        String normalizedCreatorUuid = creatorUuid == null ? null : creatorUuid.trim();

        if (normalizedName == null || normalizedName.isBlank()) {
            throw new IllegalArgumentException("Nome do grupo é obrigatório");
        }

        if (normalizedCreatorUuid == null || normalizedCreatorUuid.isBlank()) {
            throw new IllegalArgumentException("UUID do criador é obrigatório");
        }

        User creator = userRepository.findById(normalizedCreatorUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário criador não encontrado"));

        Group group = new Group();
        group.setGroupName(normalizedName);
        group.setCreatorUuid(creator.getUserUuid());
        group.setStatus("ACTIVE");

        return groupRepository.save(group);
    }

    public void deleteGroup(String groupUuid, String userUuid) {
        String normalizedGroupUuid = groupUuid == null ? null : groupUuid.trim();
        String normalizedUserUuid = userUuid == null ? null : userUuid.trim();

        if (normalizedGroupUuid == null || normalizedGroupUuid.isBlank()) {
            throw new IllegalArgumentException("UUID do grupo é obrigatório");
        }

        if (normalizedUserUuid == null || normalizedUserUuid.isBlank()) {
            throw new IllegalArgumentException("UUID do usuário é obrigatório");
        }

        Group group = groupRepository.findById(normalizedGroupUuid)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));

        if (!group.getCreatorUuid().equals(normalizedUserUuid)) {
            throw new IllegalArgumentException("Apenas o criador pode excluir o grupo");
        }

        groupRepository.delete(group);
    }

    public GroupMember addMember(String groupUuid, String authenticatedUserUuid, String targetUserUuid, String role) {
        String normalizedGroupUuid = normalizeRequired(groupUuid, "UUID do grupo é obrigatório");
        String normalizedAuthenticatedUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        String normalizedTargetUuid = normalizeRequired(targetUserUuid, "UUID do usuário é obrigatório");

        Group group = groupRepository.findById(normalizedGroupUuid)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));

        boolean isCreator = group.getCreatorUuid().equals(normalizedAuthenticatedUuid);
        boolean isAdmin = groupMemberRepository
                .findById(new GroupMemberId(normalizedGroupUuid, normalizedAuthenticatedUuid))
                .map(member -> "ADMIN".equalsIgnoreCase(member.getRole()))
                .orElse(false);

        if (!isCreator && !isAdmin) {
            throw new IllegalArgumentException("Apenas o criador ou administradores podem adicionar usuários");
        }

        userRepository.findById(normalizedTargetUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário a adicionar não encontrado"));

        GroupMemberId id = new GroupMemberId(normalizedGroupUuid, normalizedTargetUuid);
        if (groupMemberRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário já pertence ao grupo");
        }

        String normalizedRole = role == null || role.isBlank() ? "MEMBER" : role.trim().toUpperCase();
        if (!normalizedRole.equals("MEMBER") && !normalizedRole.equals("ADMIN")) {
            throw new IllegalArgumentException("Role deve ser MEMBER ou ADMIN");
        }

        return groupMemberRepository.save(new GroupMember(normalizedGroupUuid, normalizedTargetUuid, normalizedRole));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
