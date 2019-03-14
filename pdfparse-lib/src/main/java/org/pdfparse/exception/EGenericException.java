package org.pdfparse.exception;

public class EGenericException extends RuntimeException {
    /**
     * Creates a new instance of
     * <code>EGenericException</code> without detail message.
     */
    public EGenericException() {
        super();
    }

    /**
     * Constructs an instance of
     * <code>EGenericException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public EGenericException(String msg) {
        super(msg);
    }

    public EGenericException(Throwable cause) {
        super(cause);
    }

    public EGenericException(String message, Throwable cause) {
        super(message, cause);
    }

    public EGenericException(String msg, Object... args) {
        super(String.format(msg, args));
    }
}
