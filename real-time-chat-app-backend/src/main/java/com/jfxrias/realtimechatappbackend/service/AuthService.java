package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.dto.RegisterRequestDTO;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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
}
