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
    private String object;

    /**
     * violated field
     */
    private String field;

    /**
     * exception code
     */
    private String code;

    /**
     * message
     */
    private String message;

    /**
     * translated message
     */
    private String localizedMessage;

    @Builder
    public ApiValidationError(String code,
                              String message,
                              String localizedMessage) {
        this.code = code;
        this.message = message;
        this.localizedMessage = localizedMessage;
    }
}
