package me.fengorz.kiwi.tools.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds todo.demo.* properties to provide demo task definitions from YAML.
 */
@Component
@ConfigurationProperties(prefix = "todo.demo")
@Data
public class TodoDemoProperties {

    private List<TaskDef> tasks = new ArrayList<>();

    @Data
    public static class TaskDef {
        private String title;
        private String description;
        private Integer successPoints = 10;
        private Integer failPoints = -5;
        /**
         * Frequency: once | daily | weekly | monthly | custom
         */
        private String frequency = "once";
        /**
         * Used only when frequency = custom
         */
        private Integer customDays;
        /** Optional category tag, e.g., study, work, health, workout */
        private String category;
    }
}

