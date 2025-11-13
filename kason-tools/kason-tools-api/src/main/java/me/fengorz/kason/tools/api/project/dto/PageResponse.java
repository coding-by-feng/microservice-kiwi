package me.fengorz.kason.tools.api.project.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "Standard page envelope")
public class PageResponse<T> {
    @ApiModelProperty(value = "Items on the current page")
    private List<T> items;

    @ApiModelProperty(value = "Current page number (1-based)", example = "1")
    private int page;

    @ApiModelProperty(value = "Page size", example = "20")
    private int pageSize;

    @ApiModelProperty(value = "Total items across all pages", example = "123")
    private long total;
}

