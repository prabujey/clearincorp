package com.trivine.llc.api.controller;

import com.itextpdf.kernel.exceptions.PdfException;
import com.stripe.exception.StripeException;
import com.trivine.llc.api.dto.response.ErrorResponseDto;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {



    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.builder()
                        .message("Invalid input")
                        .details(ex.getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(ResponseStatusException ex) {

        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ErrorResponseDto.builder()
                            .message("Unauthorized")
                            .details(ex.getReason() != null
                                    ? ex.getReason()
                                    : "You are not allowed to access this resource.")
                            .timeStamp(LocalDateTime.now())
                            .build()
            );
        }

        // fallback for other ResponseStatusException codes if you use them
        return ResponseEntity.status(ex.getStatusCode()).body(
                ErrorResponseDto.builder()
                        .message("Error")
                        .details(ex.getReason() != null ? ex.getReason() : "Unexpected error")
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHandlerFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponseDto.builder()
                        .message("Resource not found")
                        .details(ex.getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }


    @ExceptionHandler({
            org.springframework.web.bind.MissingPathVariableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponseDto> handlePathAndTypeErrors(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.builder()
                        .message("Invalid path or parameter")
                        .details(ex.getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.builder()
                        .message("Malformed JSON request")
                        .details(ex.getMostSpecificCause().getMessage()) // more user-friendly than full trace
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }



    @ExceptionHandler({
            org.springframework.web.HttpMediaTypeNotSupportedException.class,
            org.springframework.web.HttpMediaTypeNotAcceptableException.class
    })
    public ResponseEntity<ErrorResponseDto> handleMediaType(Exception ex) {
        return ResponseEntity.status(
                ex instanceof org.springframework.web.HttpMediaTypeNotSupportedException
                        ? HttpStatus.UNSUPPORTED_MEDIA_TYPE
                        : HttpStatus.NOT_ACCEPTABLE
        ).body(
                ErrorResponseDto.builder()
                        .message("Unsupported or not acceptable media type")
                        .details(ex.getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }



    @ExceptionHandler({
            org.springframework.web.HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<ErrorResponseDto> handleMethodNotAllowed(Exception ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                ErrorResponseDto.builder()
                        .message("Method not allowed")
                        .details(ex.getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errorDetails = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ((FieldError) error).getField() + " : " + error.getDefaultMessage())
                .collect(Collectors.toList());

        log.error("Validation Error: {}", errorDetails);

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message("Validation Failed")
                .details(String.join(", ", errorDetails))
                .timeStamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException ex) {
        log.error("Resource Not Found: {}", ex.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message("Resource Not Found")
                .details(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        log.error("Unhandled Exception: ", ex);

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message("Internal Server Error")
                .details(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateEntry(DataIntegrityViolationException ex) {
        log.error("Duplicate Entry Exception: ", ex);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponseDto.builder()
                        .message("User Already Exists")
                        .details(ex.getMostSpecificCause().getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalState(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.builder()
                        .message("Invalid Input")
                        .details(ex.getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ErrorResponseDto> handleStripeException(StripeException ex) {
        log.error("StripeException: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDto.builder()
                        .message("Stripe Error")
                        .details(ex.getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler({ NoSuchAlgorithmException.class, InvalidKeyException.class })
    public ResponseEntity<ErrorResponseDto> handleCryptoExceptions(Exception ex) {
        log.error("Crypto error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponseDto.builder()
                        .message("Error during secret hash generation")
                        .details(ex.getMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(CognitoIdentityProviderException.class)
    public ResponseEntity<ErrorResponseDto> handleCognitoError(CognitoIdentityProviderException ex) {
        log.error("Cognito error: {}", ex.awsErrorDetails().errorMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponseDto.builder()
                        .message("AWS Cognito Error")
                        .details(ex.awsErrorDetails().errorMessage())
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    // 503 - infra unavailable (DB/network)
    @ExceptionHandler({
            org.springframework.transaction.CannotCreateTransactionException.class,
            org.springframework.dao.DataAccessResourceFailureException.class,
            org.springframework.dao.QueryTimeoutException.class
    })
    public ResponseEntity<ErrorResponseDto> serviceUnavailable(Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponseDto.builder().message("Service Unavailable")
                        .details(ex.getMessage())  // The details should reflect the underlying exception message
                        .timeStamp(LocalDateTime.now()).build());
    }


    // Handle PDF-related exceptions
    @ExceptionHandler(PdfException.class)
    public ResponseEntity<ErrorResponseDto> handlePdfException(PdfException ex) {
        log.error("PDF error: {}", ex.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message("PDF Generation Error")
                .details(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // Handle File-related exceptions
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponseDto> handleIOException(IOException ex) {
        log.error("File error: {}", ex.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message("File Processing Error")
                .details(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

}
