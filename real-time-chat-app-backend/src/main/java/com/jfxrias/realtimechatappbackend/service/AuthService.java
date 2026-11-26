package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.dto.RegisterRequestDTO;
import com.jfxrias.realtimechatappbackend.dto.UserResponseDTO;
import com.jfxrias.realtimechatappbackend.dto.FriendshipResponseDTO;
import com.jfxrias.realtimechatappbackend.entity.Friendship;
import com.jfxrias.realtimechatappbackend.entity.FriendshipId;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.repository.FriendshipRepository;
import com.jfxrias.realtimechatappbackend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, FriendshipRepository friendshipRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findById(String userUuid) {
        return userRepository.findById(userUuid).orElse(null);
    }

    public User register(RegisterRequestDTO request, User adminUser) {
        String name = request.name() == null ? null : request.name().trim();
        String cpf = request.cpf() == null ? null : request.cpf().trim();
        String password = request.password();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("CPF é obrigatório");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Senha deve ter pelo menos 6 caracteres");
        }

        Optional<User> existingUser = userRepository.findByCpf(cpf);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }

        User user = new User();
        user.setName(name);
        user.setCpf(cpf);
        user.setIsAdmin(Boolean.TRUE.equals(request.isAdmin()));
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public User login(String cpf, String password) {
        String normalizedCpf = cpf == null ? null : cpf.trim();
        if (normalizedCpf == null || normalizedCpf.isBlank()) {
            throw new IllegalArgumentException("CPF é obrigatório");
        }

        User user = userRepository.findByCpf(normalizedCpf)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Senha inválida");
        }

        return user;
    }

    public User updateName(String userUuid, String name) {
        String normalizedName = name == null ? null : name.trim();
        if (normalizedName == null || normalizedName.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        user.setName(normalizedName);
        return userRepository.save(user);
    }

    public void updatePassword(String userUuid, String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Senha deve ter pelo menos 6 caracteres");
        }

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    public Friendship addFriendship(String authenticatedUserUuid, String targetUserUuid) {
        String normalizedAuthenticatedUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        String normalizedTargetUuid = normalizeRequired(targetUserUuid, "UUID do usuário é obrigatório");

        if (normalizedAuthenticatedUuid.equals(normalizedTargetUuid)) {
            throw new IllegalArgumentException("Não é possível adicionar a si mesmo");
        }

        userRepository.findById(normalizedAuthenticatedUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado"));
        userRepository.findById(normalizedTargetUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário a adicionar não encontrado"));

        String firstUserUuid = normalizedAuthenticatedUuid.compareTo(normalizedTargetUuid) < 0
                ? normalizedAuthenticatedUuid : normalizedTargetUuid;
        String secondUserUuid = normalizedAuthenticatedUuid.compareTo(normalizedTargetUuid) < 0
                ? normalizedTargetUuid : normalizedAuthenticatedUuid;

        FriendshipId friendshipId = new FriendshipId(firstUserUuid, secondUserUuid);
        if (friendshipRepository.existsById(friendshipId)) {
            throw new IllegalArgumentException("Amizade já existe");
        }

        return friendshipRepository.save(new Friendship(firstUserUuid, secondUserUuid));
    }

    public UserResponseDTO getAuthenticatedUser(String authenticatedUserUuid) {
        String normalizedAuthenticatedUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        User user = userRepository.findById(normalizedAuthenticatedUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return toUserResponse(user);
    }

    public List<UserResponseDTO> listActiveUsers() {
        return userRepository.findByIsActiveTrue()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public List<FriendshipResponseDTO> listFriendships(String authenticatedUserUuid) {
        String normalizedAuthenticatedUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        userRepository.findById(normalizedAuthenticatedUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado"));

        List<Friendship> friendships = friendshipRepository.findByIdFirstUserUuidOrIdSecondUserUuid(
                normalizedAuthenticatedUuid,
                normalizedAuthenticatedUuid
        );

        List<FriendshipResponseDTO> response = new ArrayList<>();
        for (Friendship friendship : friendships) {
            String firstUserUuid = friendship.getId().getFirstUserUuid();
            String secondUserUuid = friendship.getId().getSecondUserUuid();
            String friendUserUuid = firstUserUuid.equals(normalizedAuthenticatedUuid) ? secondUserUuid : firstUserUuid;

            User friend = userRepository.findById(friendUserUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário amigo não encontrado"));

            response.add(new FriendshipResponseDTO(
                    friend.getUserUuid(),
                    friend.getName(),
                    friend.getCpf(),
                    friend.getIsAdmin(),
                    friend.getIsActive(),
                    friendship.getCreatedAt()
            ));
        }

        return response;
    }

    public void deleteFriendship(String authenticatedUserUuid, String targetUserUuid) {
        String normalizedAuthenticatedUuid = normalizeRequired(authenticatedUserUuid, "Usuário autenticado é obrigatório");
        String normalizedTargetUuid = normalizeRequired(targetUserUuid, "UUID do usuário é obrigatório");

        if (normalizedAuthenticatedUuid.equals(normalizedTargetUuid)) {
            throw new IllegalArgumentException("Não é possível remover amizade com o próprio usuário");
        }

        String firstUserUuid = normalizedAuthenticatedUuid.compareTo(normalizedTargetUuid) < 0
                ? normalizedAuthenticatedUuid : normalizedTargetUuid;
        String secondUserUuid = normalizedAuthenticatedUuid.compareTo(normalizedTargetUuid) < 0
                ? normalizedTargetUuid : normalizedAuthenticatedUuid;

        FriendshipId friendshipId = new FriendshipId(firstUserUuid, secondUserUuid);
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Amizade não encontrada"));

        friendshipRepository.delete(friendship);
    }

    private UserResponseDTO toUserResponse(User user) {
        return new UserResponseDTO(
                user.getUserUuid(),
                user.getName(),
                user.getCpf(),
                user.getIsAdmin(),
                user.getIsActive()
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
