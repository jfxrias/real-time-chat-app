package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.entity.Group;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.dto.GroupMemberResponseDTO;
import com.jfxrias.realtimechatappbackend.dto.GroupResponseDTO;
import com.jfxrias.realtimechatappbackend.repository.GroupMemberRepository;
import com.jfxrias.realtimechatappbackend.repository.GroupRepository;
import com.jfxrias.realtimechatappbackend.repository.UserRepository;
import com.jfxrias.realtimechatappbackend.entity.GroupMember;
import com.jfxrias.realtimechatappbackend.entity.GroupMemberId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public void removeMember(String groupUuid, String authenticatedUserUuid, String targetUserUuid) {
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
            throw new IllegalArgumentException("Apenas o criador ou administradores podem remover usuários");
        }

        GroupMemberId targetMembershipId = new GroupMemberId(normalizedGroupUuid, normalizedTargetUuid);
        if (!groupMemberRepository.existsById(targetMembershipId)) {
            throw new IllegalArgumentException("Usuário não pertence ao grupo");
        }

        groupMemberRepository.deleteById(targetMembershipId);
    }

    public Group updateGroupName(String groupUuid, String authenticatedUserUuid, String groupName) {
        String normalizedGroupUuid = normalizeRequired(groupUuid, "UUID do grupo é obrigatório");
        String normalizedAuthenticatedUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        String normalizedGroupName = normalizeRequired(groupName, "Nome do grupo é obrigatório");

        Group group = groupRepository.findById(normalizedGroupUuid)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));

        if (!group.getCreatorUuid().equals(normalizedAuthenticatedUuid)) {
            throw new IllegalArgumentException("Apenas o criador pode atualizar o grupo");
        }

        group.setGroupName(normalizedGroupName);
        return groupRepository.save(group);
    }

    public List<GroupResponseDTO> listUserGroups(String authenticatedUserUuid) {
        String normalizedAuthenticatedUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");

        userRepository.findById(normalizedAuthenticatedUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado"));

        List<GroupMember> memberships = groupMemberRepository.findByIdUserUuid(normalizedAuthenticatedUuid);
        Map<String, GroupResponseDTO> groupsByUuid = new LinkedHashMap<>();

        for (GroupMember membership : memberships) {
            Group group = groupRepository.findById(membership.getId().getGroupUuid())
                    .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));
            groupsByUuid.put(group.getGroupUuid(), toGroupResponse(group));
        }

        List<Group> allGroups = groupRepository.findAll();
        for (Group group : allGroups) {
            if (group.getCreatorUuid().equals(normalizedAuthenticatedUuid)) {
                groupsByUuid.put(group.getGroupUuid(), toGroupResponse(group));
            }
        }

        return new ArrayList<>(groupsByUuid.values());
    }

    public List<GroupMemberResponseDTO> listGroupMembers(String groupUuid, String authenticatedUserUuid) {
        String normalizedGroupUuid = normalizeRequired(groupUuid, "UUID do grupo é obrigatório");
        String normalizedAuthenticatedUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");

        Group group = groupRepository.findById(normalizedGroupUuid)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));

        boolean isCreator = group.getCreatorUuid().equals(normalizedAuthenticatedUuid);
        boolean isMember = groupMemberRepository.existsById(new GroupMemberId(normalizedGroupUuid, normalizedAuthenticatedUuid));
        if (!isCreator && !isMember) {
            throw new IllegalArgumentException("Usuário não participa do grupo");
        }

        List<GroupMember> members = groupMemberRepository.findByIdGroupUuid(normalizedGroupUuid);
        List<GroupMemberResponseDTO> response = new ArrayList<>();

        for (GroupMember member : members) {
            User user = userRepository.findById(member.getId().getUserUuid())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            response.add(new GroupMemberResponseDTO(
                    user.getUserUuid(),
                    user.getName(),
                    member.getRole(),
                    member.getJoinedAt()
            ));
        }

        if (isCreator && members.stream().noneMatch(member -> member.getId().getUserUuid().equals(normalizedAuthenticatedUuid))) {
            User creator = userRepository.findById(normalizedAuthenticatedUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            response.add(new GroupMemberResponseDTO(
                    creator.getUserUuid(),
                    creator.getName(),
                    "CREATOR",
                    group.getCreatedAt()
            ));
        }

        return response;
    }

    private GroupResponseDTO toGroupResponse(Group group) {
        return new GroupResponseDTO(
                group.getGroupUuid(),
                group.getGroupName(),
                group.getCreatorUuid(),
                group.getStatus(),
                group.getCreatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
