package me.fengorz.kiwi.tools.api.project.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "PATCH subset fields for project update, including stages")
public class ProjectPatchRequest extends ProjectBaseRequest {
    @ApiModelProperty(value = "Archive flag, when true marks as archived; false un-archives")
    private Boolean archived;
}
