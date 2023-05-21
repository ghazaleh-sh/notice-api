package ir.co.sadad.noticeapi.exceptions.model;

import lombok.Builder;
import lombok.Getter;

/**
 * body of exception for validation error
 */
@Getter
public class ApiValidationError implements ApiSubError {

    /**
     * obj of error
     */
    private final String object;

    /**
     * violated field
     */
    private final String field;

    /**
     * exception code
     */
    private final String code;

    /**
     * message
     */
    private final String message;

    /**
     * translated message
     */
    private final String localizedMessage;

    @Builder
    public ApiValidationError(String object,
                              String field,
                              String code,
                              String message,
                              String localizedMessage) {
        this.object = object;
        this.field = field;
        this.code = code;
        this.message = message;
        this.localizedMessage = localizedMessage;
    }
}
