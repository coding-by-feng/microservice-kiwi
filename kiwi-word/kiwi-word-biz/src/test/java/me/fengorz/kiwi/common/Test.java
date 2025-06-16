package me.fengorz.kiwi.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @Author Kason Zhan @Date 2019-09-03 10:48
 */
public class Test {

    @org.junit.jupiter.api.Test
    public void test() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Test password
        String password = "enhancer";

        // Encode the password
        String encodedPassword = encoder.encode(password);
        System.out.println("Original: " + password);
        System.out.println("Encoded:  " + encodedPassword);

        // Verify the password
        boolean matches = encoder.matches(password, encodedPassword);
        System.out.println("Matches:  " + matches);

        // Test with wrong password
        boolean wrongMatch = encoder.matches("wrong", encodedPassword);
        System.out.println("Wrong matches: " + wrongMatch);
    }
}
