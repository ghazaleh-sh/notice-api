package ir.co.sadad.noticeapi.exceptions;


import ir.co.sadad.noticeapi.exceptions.model.ApiError;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class BaseException extends RuntimeException {

    private static final long serialVersionUID = 82311953965435648L;
    protected String code;
    protected HttpStatus httpStatusCode;
    protected String extraData;
    private Map<String, Object> messageArgs;
    private ApiError apiError;

    public BaseException() {
        httpStatusCode = HttpStatus.BAD_REQUEST;
    }

    protected BaseException(Throwable cause) {
        super(cause);
        httpStatusCode = HttpStatus.BAD_REQUEST;
    }

    public BaseException(String message) {
        super(message);
        httpStatusCode = HttpStatus.BAD_REQUEST;
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
        httpStatusCode = HttpStatus.BAD_REQUEST;
    }


    public Object getMessageArg(String key) {
        if (MapUtils.isEmpty(messageArgs)) {
            return "";
        } else {
            return messageArgs.get(key);
        }
    }

    public void addMessageArg(String messageArg, Object messageVal) {
        if (MapUtils.isEmpty(this.messageArgs)) {
            this.messageArgs = new HashMap<>();
        }
        this.messageArgs.put(messageArg, messageVal);
    }

    public void addMessageExceptions(final List<BaseException> exceptions) {
        if (MapUtils.isEmpty(this.messageArgs)) {
            this.messageArgs = new HashMap<>();
        }
        this.messageArgs.put("exceptions", exceptions);
    }

    public Map<String, Object> getMessageArgs() {
        if (MapUtils.isNotEmpty(messageArgs)) {
            return messageArgs;
        }
        return null;
    }

    public abstract String getErrorCode();
    public String getExtraData() {
        return this.extraData;
    }


}