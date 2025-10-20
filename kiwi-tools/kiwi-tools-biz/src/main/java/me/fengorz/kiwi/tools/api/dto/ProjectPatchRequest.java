package me.fengorz.kiwi.tools.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import me.fengorz.kiwi.tools.api.project.dto.ProjectBaseRequest;

@Data
@ApiModel(description = "Partial update body for a Project (PATCH)")
public class ProjectPatchRequest extends ProjectBaseRequest {
    @ApiModelProperty(value = "Archive flag", example = "false")
    private Boolean archived;
}

