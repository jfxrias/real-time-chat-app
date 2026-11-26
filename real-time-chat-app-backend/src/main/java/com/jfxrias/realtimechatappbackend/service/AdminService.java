package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.entity.Group;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.dto.GroupResponseDTO;
import com.jfxrias.realtimechatappbackend.dto.UserResponseDTO;
import com.jfxrias.realtimechatappbackend.repository.GroupRepository;
import com.jfxrias.realtimechatappbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public AdminService(UserRepository userRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public User setUserActiveStatus(String adminUuid, String targetUserUuid, boolean isActive) {
        User admin = findRequiredUser(normalizeRequired(adminUuid, "UUID do administrador é obrigatório"));
        ensurePlatformAdmin(admin);

        User targetUser = findRequiredUser(normalizeRequired(targetUserUuid, "UUID do usuário é obrigatório"));
        targetUser.setIsActive(isActive);
        return userRepository.save(targetUser);
    }

    public Group setGroupActiveStatus(String adminUuid, String targetGroupUuid, boolean isActive) {
        User admin = findRequiredUser(normalizeRequired(adminUuid, "UUID do administrador é obrigatório"));
        ensurePlatformAdmin(admin);

        Group targetGroup = groupRepository.findById(normalizeRequired(targetGroupUuid, "UUID do grupo é obrigatório"))
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));
        targetGroup.setStatus(isActive ? "ACTIVE" : "INACTIVE");
        return groupRepository.save(targetGroup);
    }

    public List<UserResponseDTO> getAllUsers(String adminUuid) {
        User admin = findRequiredUser(normalizeRequired(adminUuid, "UUID do administrador é obrigatório"));
        ensurePlatformAdmin(admin);

        return userRepository.findAll().stream()
                .map(user -> new UserResponseDTO(
                        user.getUserUuid(),
                        user.getName(),
                        user.getCpf(),
                        user.getIsAdmin(),
                        user.getIsActive()
                ))
                .toList();
    }

    public List<GroupResponseDTO> getAllGroups(String adminUuid) {
        User admin = findRequiredUser(normalizeRequired(adminUuid, "UUID do administrador é obrigatório"));
        ensurePlatformAdmin(admin);

        return groupRepository.findAll().stream()
                .map(group -> new GroupResponseDTO(
                        group.getGroupUuid(),
                        group.getGroupName(),
                        group.getCreatorUuid(),
                        group.getStatus(),
                        group.getCreatedAt()
                ))
                .toList();
    }

    private User findRequiredUser(String userUuid) {
        return userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    private void ensurePlatformAdmin(User admin) {
        if (!Boolean.TRUE.equals(admin.getIsAdmin())) {
            throw new IllegalArgumentException("Apenas administradores da plataforma podem realizar esta operação");
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
