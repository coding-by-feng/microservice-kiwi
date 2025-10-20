package me.fengorz.kiwi.tools.model.todo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("todo_trash")
public class TodoTrash {
    private String id;
    private Integer userId;
    private String title;
    private String description;
    private Integer successPoints;
    private Integer failPoints;
    private String frequency;
    private Integer customDays;
    private String status;
    private LocalDateTime originalDate;
    private LocalDateTime deletedDate;
}

