package com.urlshortener.url;

import java.util.Map;

import com.urlshortener.auth.EmailAlreadyExistsException;
import com.urlshortener.auth.InvalidCredentialsException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({InvalidUrlException.class, InvalidAliasException.class,
            EmailAlreadyExistsException.class, InvalidCredentialsException.class,
            MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, String>> badRequest(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldError().getDefaultMessage()
                : exception.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    ResponseEntity<Map<String, String>> conflict(AliasAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(UrlNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(UrlNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }
}
