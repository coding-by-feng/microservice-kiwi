package me.fengorz.kiwi.tools.model;

import lombok.Data;

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
    private String status; // 未开始, 进行中, 已完成
    private String todayTask; // text
    private String progressNote; // text
    private String photoUrl; // public URL or empty
    private String createdAt; // ISO 8601 UTC
}
