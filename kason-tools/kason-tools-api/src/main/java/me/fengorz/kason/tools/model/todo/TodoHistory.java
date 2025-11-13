package me.fengorz.kason.tools.model.todo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("todo_history")
public class TodoHistory {
    private String id;
    private Integer userId;
    private String taskId;
    private String title;
    private String description;
    private Integer successPoints;
    private Integer failPoints;
    private String status; // success/fail
    private Integer pointsApplied;
    private LocalDateTime completedAt;
}
