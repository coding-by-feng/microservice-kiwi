package me.fengorz.kason.tools.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_photos")
public class ProjectPhoto {
    private String id;

    private String projectId;

    private String dfsFileId; // e.g. group/path

    private String token;     // URL-safe token derived from dfsFileId

    private String contentType;

    private Long size;

    private Integer sortOrder;

    private String caption;

    private LocalDateTime createdAt;
}
