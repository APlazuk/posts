package pl.aplazuk.postbackend.exception;

public class NoPostFoundException extends RuntimeException {

    public NoPostFoundException(String message) {
        super(message);
    }

    public NoPostFoundException(String customMessage, String customCause) {
        super(customMessage + " |Cause: " + customCause);
    }
}
