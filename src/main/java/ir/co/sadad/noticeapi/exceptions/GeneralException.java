package ir.co.sadad.noticeapi.exceptions;

import org.springframework.http.HttpStatus;

public final class GeneralException extends BaseException {

    private static final long serialVersionUID = -7456251161529545902L;

    public GeneralException(String code) {
        this.code = code;
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public GeneralException(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatusCode = httpStatus;
    }

    public GeneralException(String message, String code) {
        super(message);
        this.code = code;
    }

    @Override
    public String getErrorCode() {
        return this.code;
    }
}
