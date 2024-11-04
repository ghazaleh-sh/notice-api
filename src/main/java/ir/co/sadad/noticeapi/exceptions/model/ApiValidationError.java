package ir.co.sadad.noticeapi.exceptions.model;

import lombok.Builder;
import lombok.Getter;

/**
 * body of exception for validation error
 */
@Getter
public class ApiValidationError implements ApiSubError {

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
    public ApiValidationError(String code,
                              String message,
                              String localizedMessage) {
        this.code = code;
        this.message = message;
        this.localizedMessage = localizedMessage;
    }
}
