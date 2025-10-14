package me.fengorz.kiwi.tools.exception;

import me.fengorz.kiwi.tools.api.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException ex) {
        ApiError.ErrorBody body = new ApiError.ErrorBody(ex.getCode(), ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(body));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiError> handleValidation(Exception ex) {
        Map<String, Object> details = new HashMap<>();
        String message = "Validation failed";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError.ErrorBody body = new ApiError.ErrorBody("validation_error", message, details);
        return ResponseEntity.status(status).body(new ApiError(body));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupported(HttpMediaTypeNotSupportedException ex) {
        ApiError.ErrorBody body = new ApiError.ErrorBody("unsupported_media_type", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new ApiError(body));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ApiError.ErrorBody body = new ApiError.ErrorBody("validation_error", "Invalid parameter: " + ex.getName(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(body));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        ApiError.ErrorBody body = new ApiError.ErrorBody("internal_error", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(body));
    }
}
