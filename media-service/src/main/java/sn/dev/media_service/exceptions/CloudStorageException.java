package sn.dev.media_service.exceptions;

/**
 * Runtime exception used to signal cloud storage upload errors.
 */
public class CloudStorageException extends RuntimeException {
    public CloudStorageException(String message) {
        super(message);
    }

    public CloudStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

