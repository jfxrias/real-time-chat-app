package com.jfxrias.realtimechatappbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report")
public class Report {
    @Id
    @Column(name = "report_uuid", length = 36, nullable = false, updatable = false)
    private String reportUuid = UUID.randomUUID().toString();

    @Column(name = "reporter_uuid", length = 36, nullable = false, updatable = false)
    private String reporterUuid;

    @Column(name = "reported_uuid", length = 36, nullable = false, updatable = false)
    private String reportedUuid;

    @Column(name = "group_uuid", length = 36, nullable = false, updatable = false)
    private String groupUuid;

    @Column(name = "reason", length = 255, nullable = false)
    private String reason;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    public Report() {
    }

    public Report(String reporterUuid, String reportedUuid, String groupUuid, String reason) {
        this.reporterUuid = reporterUuid;
        this.reportedUuid = reportedUuid;
        this.groupUuid = groupUuid;
        this.reason = reason;
    }

    public String getReportUuid() { return reportUuid; }
    public void setReportUuid(String reportUuid) { this.reportUuid = reportUuid; }
    public String getReporterUuid() { return reporterUuid; }
    public void setReporterUuid(String reporterUuid) { this.reporterUuid = reporterUuid; }
    public String getReportedUuid() { return reportedUuid; }
    public void setReportedUuid(String reportedUuid) { this.reportedUuid = reportedUuid; }
    public String getGroupUuid() { return groupUuid; }
    public void setGroupUuid(String groupUuid) { this.groupUuid = groupUuid; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
}
