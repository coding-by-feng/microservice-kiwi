package me.fengorz.kiwi.tools.api.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "Base fields for project create/update requests")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ProjectBaseRequest {
    @ApiModelProperty(value = "Project name", example = "Kitchen Remodel")
    private String name;

    @ApiModelProperty(value = "Client name", example = "张三")
    private String clientName;

    @ApiModelProperty(value = "Client phone", example = "+86-13800000000")
    private String clientPhone;

    @ApiModelProperty(value = "Project address", example = "上海市徐汇区...")
    private String address;

    @ApiModelProperty(value = "Sales person", example = "Alice")
    private String salesPerson;

    @ApiModelProperty(value = "Installer", example = "Bob")
    private String installer;

    @ApiModelProperty(value = "Team members free text", example = "Tom, Jerry")
    private String teamMembers;

    @ApiModelProperty(value = "Start date (YYYY-MM-DD)", example = "2025-10-01")
    private String startDate;

    @ApiModelProperty(value = "End date (YYYY-MM-DD)", example = "2025-10-15")
    private String endDate;

    @ApiModelProperty(value = "Project status code", allowableValues = "not_started,in_progress,completed", example = "in_progress")
    private String status;

    @ApiModelProperty(value = "Today's task note", example = "Install cabinets")
    private String todayTask;

    @ApiModelProperty(value = "Progress note", example = "Tile delivered")
    private String progressNote;
}

