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
    GLASS_ORDERED("glass_ordered", "玻璃已下单"),
    DOORS_WINDOWS_PRODUCED("doors_windows_produced", "门窗已生产"),
    DOORS_WINDOWS_DELIVERED("doors_windows_delivered", "门窗已送货"),
    DOORS_WINDOWS_INSTALLED("doors_windows_installed", "门窗已安装"),
    FINAL_PAYMENT_RECEIVED("final_payment_received", "尾款已收到");

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
        ProjectStatus byCode = fromCode(s);
        if (byCode != null) return byCode;
        switch (s) {
            case "玻璃已下单": return GLASS_ORDERED;
            case "门窗已生产": return DOORS_WINDOWS_PRODUCED;
            case "门窗已送货": return DOORS_WINDOWS_DELIVERED;
            case "门窗已安装": return DOORS_WINDOWS_INSTALLED;
            case "尾款已收到": return FINAL_PAYMENT_RECEIVED;
            default:
                String u = s.toUpperCase(Locale.ROOT);
                if (u.contains("GLASS") && u.contains("ORDER")) return GLASS_ORDERED;
                if ((u.contains("DOOR") || u.contains("WINDOW")) && (u.contains("PRODUCED") || u.contains("MADE") || u.contains("MANUFACTURED"))) return DOORS_WINDOWS_PRODUCED;
                if ((u.contains("DOOR") || u.contains("WINDOW")) && u.contains("DELIVER")) return DOORS_WINDOWS_DELIVERED;
                if ((u.contains("DOOR") || u.contains("WINDOW")) && u.contains("INSTALL")) return DOORS_WINDOWS_INSTALLED;
                if (u.contains("FINAL") && (u.contains("RECEIV") || u.contains("PAID") || u.contains("PAYMENT"))) return FINAL_PAYMENT_RECEIVED;
                try { return ProjectStatus.valueOf(u); } catch (Exception ignored) {}
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
