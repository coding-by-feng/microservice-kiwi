package me.fengorz.kiwi.tools.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "Project photo response")
public class ProjectPhotoResponse {
    @ApiModelProperty(value = "Photo id", example = "pht_123")
    private String id;

    @ApiModelProperty(value = "Project id", example = "123")
    private String projectId;

    @ApiModelProperty(value = "DFS file id (group/path)")
    private String dfsFileId;

    @ApiModelProperty(value = "Download token")
    private String token;

    @ApiModelProperty(value = "MIME type", example = "image/jpeg")
    private String contentType;

    @ApiModelProperty(value = "File size in bytes", example = "204800")
    private Long size;

    @ApiModelProperty(value = "Sort order", example = "1")
    private Integer sortOrder;

    @ApiModelProperty(value = "Caption")
    private String caption;

    @ApiModelProperty(value = "Created at")
    private LocalDateTime createdAt;
}

