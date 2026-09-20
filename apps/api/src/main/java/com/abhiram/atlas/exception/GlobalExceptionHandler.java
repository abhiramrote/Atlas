package com.abhiram.atlas.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.abhiram.atlas.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {

        ErrorResponse response =
                new ErrorResponse(
                        ex.getMessage(),
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(response);
    }
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleBadRequest(
                IllegalArgumentException ex
        ) {
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
            );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
}