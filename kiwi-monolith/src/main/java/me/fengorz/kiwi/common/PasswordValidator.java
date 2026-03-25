/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Password strength validator.
 * Enforces minimum complexity requirements for user passwords.
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 10;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=]");

    private PasswordValidator() {
    }

    /**
     * Validate password strength. Returns null if valid, or a specific error message describing what is missing.
     */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }

        List<String> missing = new ArrayList<>();

        if (password.length() < MIN_LENGTH) {
            missing.add("at least " + MIN_LENGTH + " characters");
        }
        if (!UPPERCASE.matcher(password).find()) {
            missing.add("at least 1 uppercase letter");
        }
        if (!LOWERCASE.matcher(password).find()) {
            missing.add("at least 1 lowercase letter");
        }
        if (!DIGIT.matcher(password).find()) {
            missing.add("at least 1 digit");
        }
        if (!SPECIAL.matcher(password).find()) {
            missing.add("at least 1 special character (!@#$%^&*()_+-=)");
        }

        if (missing.isEmpty()) {
            return null;
        }

        return "Password must contain: " + String.join(", ", missing);
    }
}
