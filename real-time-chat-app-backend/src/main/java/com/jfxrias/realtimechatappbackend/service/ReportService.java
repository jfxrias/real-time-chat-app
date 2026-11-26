package com.jfxrias.realtimechatappbackend.service;

import com.jfxrias.realtimechatappbackend.entity.Group;
import com.jfxrias.realtimechatappbackend.entity.Report;
import com.jfxrias.realtimechatappbackend.entity.User;
import com.jfxrias.realtimechatappbackend.repository.GroupRepository;
import com.jfxrias.realtimechatappbackend.repository.ReportRepository;
import com.jfxrias.realtimechatappbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public ReportService(ReportRepository reportRepository,
                         UserRepository userRepository,
                         GroupRepository groupRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional
    public Report createReport(String reporterUuid, String reportedUuid, String groupUuid, String reason) {
        String normalizedReporter = normalizeRequired(reporterUuid, "Usuário autenticado é obrigatório");
        String normalizedReported = normalizeRequired(reportedUuid, "UUID do usuário reportado é obrigatório");
        String normalizedGroup = normalizeRequired(groupUuid, "UUID do grupo é obrigatório");
        String normalizedReason = normalizeReason(reason);

        User reporter = userRepository.findById(normalizedReporter)
                .orElseThrow(() -> new IllegalArgumentException("Usuário remetente não encontrado"));
        if (!Boolean.TRUE.equals(reporter.getIsActive())) {
            throw new IllegalArgumentException("Usuário remetente não está ativo");
        }

        userRepository.findById(normalizedReported)
                .orElseThrow(() -> new IllegalArgumentException("Usuário reportado não encontrado"));
        if (normalizedReporter.equals(normalizedReported)) {
            throw new IllegalArgumentException("Usuário não pode reportar a si mesmo");
        }

        Group group = groupRepository.findById(normalizedGroup)
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));
        if (!"ACTIVE".equalsIgnoreCase(group.getStatus())) {
            throw new IllegalArgumentException("O grupo não está ativo");
        }

        return reportRepository.save(new Report(normalizedReporter, normalizedReported, normalizedGroup, normalizedReason));
    }

    public List<Report> getAllReports(String adminUuid) {
        String normalizedAdminUuid = normalizeRequired(adminUuid, "UUID do administrador é obrigatório");
        User admin = userRepository.findById(normalizedAdminUuid)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!Boolean.TRUE.equals(admin.getIsAdmin())) {
            throw new IllegalArgumentException("Apenas administradores da plataforma podem realizar esta operação");
        }

        return reportRepository.findAllByOrderByReportedAtDesc();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeReason(String reason) {
        String normalized = normalizeRequired(reason, "Motivo é obrigatório");
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Motivo deve ter no máximo 255 caracteres");
        }
        return normalized;
    }
}
