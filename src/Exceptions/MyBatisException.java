package Exceptions;

public class MyBatisException extends RuntimeException {
    public MyBatisException() {
    }

    public MyBatisException(String message) {
        super(message);
    }

    public MyBatisException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyBatisException(Throwable cause) {
        super(cause);
    }

    public MyBatisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
