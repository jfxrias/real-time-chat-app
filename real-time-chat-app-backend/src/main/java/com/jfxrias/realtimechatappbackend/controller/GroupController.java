package com.jfxrias.realtimechatappbackend.controller;

import com.jfxrias.realtimechatappbackend.dto.CreateGroupRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.GroupResponseDTO;
import com.jfxrias.realtimechatappbackend.entity.Group;
import com.jfxrias.realtimechatappbackend.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/groups", "/group"})
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<?> createGroup(
            @Valid @RequestBody(required = false) CreateGroupRequestDTO request,
            @RequestHeader(value = "user-uuid", required = false) String userUuidHeader,
            @RequestHeader(value = "creator-uuid", required = false) String creatorUuidHeader,
            @RequestParam(value = "userUuid", required = false) String userUuidParam,
            @RequestParam(value = "creatorUuid", required = false) String creatorUuidParam
    ) {
        try {
            String creatorUuid = resolveCreatorUuid(request, userUuidHeader, creatorUuidHeader, userUuidParam, creatorUuidParam);
            Group group = groupService.createGroup(
                    request == null ? null : request.groupName(),
                    creatorUuid
            );
            return ResponseEntity.ok(toResponse(group));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping({"", "/{groupUuid}", "/delete/{groupUuid}"})
    public ResponseEntity<?> deleteGroup(
            @PathVariable(value = "groupUuid", required = false) String pathGroupUuid,
            @RequestParam(value = "groupUuid", required = false) String queryGroupUuid,
            @RequestHeader(value = "user-uuid", required = false) String userUuidHeader,
            @RequestHeader(value = "creator-uuid", required = false) String creatorUuidHeader,
            @RequestParam(value = "userUuid", required = false) String userUuidParam,
            @RequestParam(value = "creatorUuid", required = false) String creatorUuidParam
    ) {
        try {
            String groupUuid = pathGroupUuid != null ? pathGroupUuid : queryGroupUuid;
            String userUuid = resolveCreatorUuid(null, userUuidHeader, creatorUuidHeader, userUuidParam, creatorUuidParam);
            groupService.deleteGroup(groupUuid, userUuid);
            return ResponseEntity.ok(Map.of("message", "Grupo excluído com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private String resolveCreatorUuid(
            CreateGroupRequestDTO request,
            String userUuidHeader,
            String creatorUuidHeader,
            String userUuidParam,
            String creatorUuidParam
    ) {
        if (request != null) {
            if (request.creatorUuid() != null && !request.creatorUuid().isBlank()) {
                return request.creatorUuid();
            }
            if (request.userUuid() != null && !request.userUuid().isBlank()) {
                return request.userUuid();
            }
        }

        if (userUuidHeader != null && !userUuidHeader.isBlank()) {
            return userUuidHeader;
        }
        if (creatorUuidHeader != null && !creatorUuidHeader.isBlank()) {
            return creatorUuidHeader;
        }
        if (userUuidParam != null && !userUuidParam.isBlank()) {
            return userUuidParam;
        }
        if (creatorUuidParam != null && !creatorUuidParam.isBlank()) {
            return creatorUuidParam;
        }

        return null;
    }

    private GroupResponseDTO toResponse(Group group) {
        return new GroupResponseDTO(
                group.getGroupUuid(),
                group.getGroupName(),
                group.getCreatorUuid(),
                group.getStatus(),
                group.getCreatedAt()
        );
    }
}
