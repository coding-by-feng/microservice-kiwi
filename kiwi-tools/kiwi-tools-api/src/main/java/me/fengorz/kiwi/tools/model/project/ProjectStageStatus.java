package me.fengorz.kiwi.tools.model.project;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * One-to-one per Project stage completion flags.
 * Table: project_stage_status (pk: project_id)
 */
@Data
@TableName("project_stage_status")
public class ProjectStageStatus {
    @TableId(value = "project_id")
    private String projectId;

    // 玻璃
    private Boolean glass;
    private String glassRemark;
    // 框架
    private Boolean frame;
    private String frameRemark;
    // 采购
    private Boolean purchase;
    private String purchaseRemark;
    // 运输
    private Boolean transport;
    private String transportRemark;
    // 安装
    private Boolean install;
    private String installRemark;
    // 维修
    private Boolean repair;
    private String repairRemark;

    public static ProjectStageStatus empty(String projectId) {
        ProjectStageStatus s = new ProjectStageStatus();
        s.setProjectId(projectId);
        s.setGlass(false);
        s.setFrame(false);
        s.setPurchase(false);
        s.setTransport(false);
        s.setInstall(false);
        s.setRepair(false);
        return s;
    }
}
