package ir.co.sadad.noticeapi.exceptions.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

import javax.validation.ConstraintViolation;
import java.util.*;

/**
 * main body of exception
 */
@Getter
@Builder
public class ApiError {

    /**
     * code of exception - 400 ~ 500
     */
    private final HttpStatus status;

    /**
     * when error occurred
     */
    private final Long timestamp;

    /**
     * code of exception
     */
    private final String code;

    /**
     * message of exception
     */
    private final String message;

    /**
     * translated message
     */
    private final String localizedMessage;

    /**
     * list of sub errors
     */
    private List<ApiSubError> subErrors;

    /**
     * meta data
     */
    @Setter
    private Map<String, Object> meta;

    /**
     * extra date
     */
    private final String extraData;


    @JsonAnyGetter
    public Map<String, Object> getMeta() {
        if (meta == null) {
            meta = new HashMap<>();
        }

        return meta;
    }

    private void addSubError(ApiSubError subError) {
        if (subErrors == null) {
            subErrors = new ArrayList<>();
        }
        subErrors.add(subError);
    }

    public void addValidationError(String field, String object,
                                   String code, String message, String localizedMessage) {
        addSubError(ApiValidationError.builder()
                .field(field)
                .localizedMessage(localizedMessage)
                .message(message)
                .code(code)
                .object(object)
                .build());
    }

    /**
     * Utility method for adding error of ConstraintViolation. Usually when a @Validated validation fails.
     *
     * @param cv the ConstraintViolation
     */
    private void addValidationError(ConstraintViolation<?> cv) {
        this.addValidationError(null, cv.getRootBeanClass().getSimpleName(),
                ((PathImpl) cv.getPropertyPath()).getLeafNode().asString(),
                cv.getInvalidValue() == null ? "" : cv.getInvalidValue().toString(),
                cv.getMessage());
    }

    public void addValidationError(FieldError fieldError) {
        this.addValidationError("", fieldError.getObjectName(), fieldError.getField(),
                fieldError.getRejectedValue() == null ? ""
                        : fieldError.getRejectedValue().toString(),
                fieldError.getDefaultMessage());
    }


}