package me.fengorz.kiwi.tools.model.project;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Project {
    private String id; // string or number; we will use numeric string
    private String projectCode; // unique, server-generated e.g. P-001
    private String name; // 1-100
    private String clientName; // 0-100
    private String clientPhone; // 0-30
    private String address; // 0-200
    private String salesPerson; // 0-100
    private String installer; // 0-100
    private String teamMembers; // free text
    private String startDate; // YYYY-MM-DD
    private String endDate; // YYYY-MM-DD
    private ProjectStatus status; // glass_ordered, doors_windows_produced, doors_windows_delivered, doors_windows_installed, final_payment_received
    private String todayTask; // text
    private String progressNote; // text
    private String changeNote; // 项目变更
    private LocalDateTime createdAt; // ISO 8601 UTC
    private Boolean archived; // default in DB is false
}
