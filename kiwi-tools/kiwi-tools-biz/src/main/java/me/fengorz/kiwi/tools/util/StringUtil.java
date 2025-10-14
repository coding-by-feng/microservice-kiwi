package me.fengorz.kiwi.tools.util;

public class StringUtil {
    public static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static boolean hasControlChars(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32) {
                return true;
            }
        }
        return false;
    }
}

