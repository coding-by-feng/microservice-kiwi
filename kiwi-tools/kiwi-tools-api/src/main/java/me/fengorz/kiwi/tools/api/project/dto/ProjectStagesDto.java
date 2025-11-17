package me.fengorz.kiwi.tools.api.project.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "Per-stage completion flags for a Project")
public class ProjectStagesDto {
    @ApiModelProperty(value = "玻璃是否完成(Y/N)")
    private Boolean glass;
    @ApiModelProperty(value = "玻璃备注")
    private String glassRemark;

    @ApiModelProperty(value = "框架是否完成(Y/N)")
    private Boolean frame;
    @ApiModelProperty(value = "框架备注")
    private String frameRemark;

    @ApiModelProperty(value = "采购是否完成(Y/N)")
    private Boolean purchase;
    @ApiModelProperty(value = "采购备注")
    private String purchaseRemark;

    @ApiModelProperty(value = "运输是否完成(Y/N)")
    private Boolean transport;
    @ApiModelProperty(value = "运输备注")
    private String transportRemark;

    @ApiModelProperty(value = "安装是否完成(Y/N)")
    private Boolean install;
    @ApiModelProperty(value = "安装备注")
    private String installRemark;

    @ApiModelProperty(value = "维修是否完成(Y/N)")
    private Boolean repair;
    @ApiModelProperty(value = "维修备注")
    private String repairRemark;
}
