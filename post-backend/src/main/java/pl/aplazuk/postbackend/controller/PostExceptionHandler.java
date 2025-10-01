package pl.aplazuk.postbackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.aplazuk.postbackend.exception.NoPostFoundException;
import pl.aplazuk.postbackend.exception.PostServiceTemporaryUnavailable;

@RestControllerAdvice
public class PostExceptionHandler {

    @ExceptionHandler(NoPostFoundException.class)
    public ResponseEntity<String> handleNoPostFoundException(NoPostFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(PostServiceTemporaryUnavailable.class)
    public ResponseEntity<String> handleServerErrorException(PostServiceTemporaryUnavailable exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Service temporary unavailable: " + exception.getMessage());
    }
}
