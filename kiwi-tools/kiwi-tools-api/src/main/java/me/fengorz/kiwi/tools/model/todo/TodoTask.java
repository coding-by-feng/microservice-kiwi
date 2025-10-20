package me.fengorz.kiwi.tools.model.todo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("todo_task")
public class TodoTask {
    private String id;
    private Integer userId;
    private String title;
    private String description;
    private Integer successPoints;
    private Integer failPoints;
    private String frequency; // once,daily,weekly,monthly,custom
    private Integer customDays;
    private String status; // pending,success,fail
    private String metadata; // JSON text
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}

