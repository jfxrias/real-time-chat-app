package com.jfxrias.realtimechatappbackend.repository;

import com.jfxrias.realtimechatappbackend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, String> {
    List<Report> findAllByOrderByReportedAtDesc();
}
