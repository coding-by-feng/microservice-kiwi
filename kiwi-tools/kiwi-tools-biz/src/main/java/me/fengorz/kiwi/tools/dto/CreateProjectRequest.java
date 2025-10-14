package me.fengorz.kiwi.tools.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateProjectRequest {
    @NotBlank
    @Size(min = 1, max = 100)
    private String name;

    @Size(max = 100)
    private String client_name;

    @Size(max = 30)
    private String client_phone;

    @Size(max = 200)
    private String address;

    @Size(max = 100)
    private String sales_person;

    @Size(max = 100)
    private String installer;

    private String team_members;

    // YYYY-MM-DD
    private String start_date;
    private String end_date;

    private String status; // default 未开始

    private String today_task;
    private String progress_note;
}

