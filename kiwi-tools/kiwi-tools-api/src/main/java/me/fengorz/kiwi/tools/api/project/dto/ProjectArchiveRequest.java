package me.fengorz.kiwi.tools.api.project.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "Archive/unarchive request body")
public class ProjectArchiveRequest {
    @ApiModelProperty(value = "true to archive, false to unarchive", example = "true")
    private Boolean archived;
}

