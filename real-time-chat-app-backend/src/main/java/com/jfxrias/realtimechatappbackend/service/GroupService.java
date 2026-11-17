package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.entity.Group;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.repository.GroupRepository;
import com.jfxrias.realtimechatappbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
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
}
