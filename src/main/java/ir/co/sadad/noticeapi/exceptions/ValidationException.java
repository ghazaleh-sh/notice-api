package ir.co.sadad.noticeapi.exceptions;

import org.springframework.http.HttpStatus;

public class ValidationException extends BaseException {

    private static final long serialVersionUID = -7456251161529545902L;

    public ValidationException(String code) {
        this.code = code;
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ValidationException(String code, String subMessage, HttpStatus httpStatus) {
        super(subMessage);
        this.code = code;
        this.httpStatusCode = httpStatus;
    }

    public ValidationException(String message, String code) {
        super(message);
        this.code = code;
    }

    @Override
    public String getErrorCode() {
        return this.code;
    }
}
