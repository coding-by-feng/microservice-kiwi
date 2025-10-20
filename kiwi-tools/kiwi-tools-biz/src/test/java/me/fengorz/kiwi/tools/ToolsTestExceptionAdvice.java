package me.fengorz.kiwi.tools;

import me.fengorz.kiwi.tools.exception.ToolsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ToolsTestExceptionAdvice {

    @ExceptionHandler(ToolsException.class)
    public ResponseEntity<Map<String, Object>> handleToolsException(ToolsException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        if (ex.getDetails() != null) body.put("details", ex.getDetails());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}

