package pl.aplazuk.postbackend.exception;

import org.springframework.http.HttpStatusCode;

public class PostServiceTemporaryUnavailable extends RuntimeException {

    public PostServiceTemporaryUnavailable(String message) {
        super(message);
    }

    public PostServiceTemporaryUnavailable(HttpStatusCode httpStatus, String statusText) {
        super(httpStatus + " |Cause: " + statusText);
    }
}
