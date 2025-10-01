package pl.aplazuk.postbackend.exception;

public class JsonFileProcessingException extends RuntimeException {

    public JsonFileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
