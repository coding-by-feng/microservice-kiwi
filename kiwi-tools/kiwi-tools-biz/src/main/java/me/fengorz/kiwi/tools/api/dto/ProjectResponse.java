package me.fengorz.kiwi.tools.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "Project response")
public class ProjectResponse {
    @ApiModelProperty(value = "Primary id", example = "123")
    private String id;

    @ApiModelProperty(value = "Unique project code", example = "P-001")
    private String projectCode;

    @ApiModelProperty(value = "Project name", example = "Kitchen Remodel")
    private String name;

    @ApiModelProperty(value = "Client name", example = "张三")
    private String clientName;

    @ApiModelProperty(value = "Client phone", example = "+86-13800000000")
    private String clientPhone;

    @ApiModelProperty(value = "Address")
    private String address;

    @ApiModelProperty(value = "Sales person")
    private String salesPerson;

    @ApiModelProperty(value = "Installer")
    private String installer;

    @ApiModelProperty(value = "Team members")
    private String teamMembers;

    @ApiModelProperty(value = "Start date (YYYY-MM-DD)")
    private String startDate;

    @ApiModelProperty(value = "End date (YYYY-MM-DD)")
    private String endDate;

    @ApiModelProperty(value = "Status code", allowableValues = "not_started,in_progress,completed")
    private String status;

    @ApiModelProperty(value = "Today's task")
    private String todayTask;

    @ApiModelProperty(value = "Progress note")
    private String progressNote;

    @ApiModelProperty(value = "Creation timestamp (ISO 8601)")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "Archive flag")
    private Boolean archived;
}

