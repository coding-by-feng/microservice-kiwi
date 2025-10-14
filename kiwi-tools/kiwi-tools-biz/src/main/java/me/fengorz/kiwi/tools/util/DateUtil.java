package me.fengorz.kiwi.tools.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtil {
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static boolean isValidDate(String s) {
        if (s == null || s.trim().isEmpty()) return false;
        try {
            LocalDate.parse(s.trim(), DATE_FMT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static LocalDate parseDate(String s) {
        return LocalDate.parse(s.trim(), DATE_FMT);
    }

    public static String nowIso() {
        return Instant.now().toString();
    }
}

