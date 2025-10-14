package me.fengorz.kiwi.tools.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateProjectRequest {
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

    private String start_date;
    private String end_date;

    private String status;

    private String today_task;
    private String progress_note;
    private String photo_url; // allow clearing or setting via update
}

