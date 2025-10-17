package me.fengorz.kiwi.tools.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;

@Getter
public enum ProjectStatus implements IEnum<String> {
    NOT_STARTED("not_started", "未开始"),
    IN_PROGRESS("in_progress", "施工中"),
    COMPLETED("completed", "完成");

    @EnumValue
    private final String code;
    private final String labelZh;

    ProjectStatus(String code, String labelZh) {
        this.code = code;
        this.labelZh = labelZh;
    }

    @Override
    @JsonValue
    public String getValue() {
        return code;
    }

    public static ProjectStatus fromCode(String code) {
        if (code == null || code.isEmpty()) return null;
        String c = code.trim().toLowerCase(Locale.ROOT);
        for (ProjectStatus s : values()) {
            if (s.code.equals(c)) return s;
        }
        return null;
    }

    @JsonCreator
    public static ProjectStatus fromInput(Object input) {
        if (input == null) return null;
        String s = String.valueOf(input).trim();
        // try code first
        ProjectStatus byCode = fromCode(s);
        if (byCode != null) return byCode;
        // accept legacy Chinese values
        switch (s) {
            case "未开始":
                return NOT_STARTED;
            case "进行中":
            case "施工中":
                return IN_PROGRESS;
            case "已完成":
            case "完成":
                return COMPLETED;
            default:
                try {
                    return ProjectStatus.valueOf(s.toUpperCase(Locale.ROOT));
                } catch (Exception ignored) {}
        }
        return null;
    }

    public static boolean isValid(Object input) {
        return fromInput(input) != null;
    }

    public static String[] allowedCodes() {
        return Arrays.stream(values()).map(ProjectStatus::getCode).toArray(String[]::new);
    }
}
