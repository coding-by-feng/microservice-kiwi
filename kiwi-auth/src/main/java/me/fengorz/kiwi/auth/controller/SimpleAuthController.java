package me.fengorz.kiwi.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.auth.config.SimpleAuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oauth/one-time")
@RequiredArgsConstructor
public class SimpleAuthController {

    private final SimpleAuthProperties props;

    /**
     * Simple verification endpoint.
     * Example: POST /oauth/verify-code {"code":"123456"}
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verify(@RequestBody(required = false) Map<String, String> body,
                                    @RequestParam(value = "code", required = false) String codeParam) {
        String configured = props.getPasscode();
        if (!StringUtils.hasText(configured)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("passcode not configured");
        }

        String code = (body != null ? body.get("code") : null);
        if (!StringUtils.hasText(code)) {
            code = codeParam;
        }
        if (!StringUtils.hasText(code)) {
            return ResponseEntity.badRequest().body("code is required");
        }
        if (!code.matches("\\d{6}")) {
            return ResponseEntity.badRequest().body("code must be 6 digits");
        }

        boolean ok = constantTimeEquals(configured, code);
        if (ok) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid code");
    }

    private static boolean constantTimeEquals(String a, String b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] da = md.digest(a.getBytes(StandardCharsets.UTF_8));
            byte[] db = md.digest(b.getBytes(StandardCharsets.UTF_8));
            if (da.length != db.length) return false;
            int result = 0;
            for (int i = 0; i < da.length; i++) {
                result |= da[i] ^ db[i];
            }
            return result == 0;
        } catch (NoSuchAlgorithmException e) {
            return a.equals(b);
        }
    }
}

