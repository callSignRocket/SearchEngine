package searchengine.controllers;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.exception.ErrMessage;

@RestControllerAdvice
public class ApiErrorController {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ChangeSetPersister.NotFoundException.class)
    public ErrMessage handleException(ChangeSetPersister.NotFoundException exception) {
        return new ErrMessage(exception.getMessage());
    }
}
