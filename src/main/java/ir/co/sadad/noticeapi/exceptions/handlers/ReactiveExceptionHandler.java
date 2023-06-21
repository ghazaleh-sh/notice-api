package ir.co.sadad.noticeapi.exceptions.handlers;

import ir.co.sadad.noticeapi.exceptions.BaseException;
import ir.co.sadad.noticeapi.exceptions.GeneralException;
import ir.co.sadad.noticeapi.exceptions.ValidationException;
import ir.co.sadad.noticeapi.exceptions.model.ApiError;
import ir.co.sadad.noticeapi.exceptions.model.ApiSubError;
import ir.co.sadad.noticeapi.exceptions.model.ApiValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
public class ReactiveExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final String ERROR_GENERAL_VALIDATION = "PPA.general.validation";
    private static final String ERROR_GENERAL_DB_CONSTRAINT_VIOLATION = "PPA.general.db.constraint.violation";
    private static final String ERROR_GENERAL_DB_CONNECTION_EXCEPTION = "PPA.general.db.connection.exception";
    private static final String ERROR_GENERAL_DB_EXCEPTION = "PPA.general.db.execute.exception";
    private static final String ERROR_METHOD_ARGUMENT_NOT_VALID = "PPA.general.validator.EBP40000002";
    private static final String ERROR_CONSTRAINT_VIOLATION = "PPA.general.validator.EBP40000001";
    private static final String ERROR_INTERNAL_SERVER = "PPA.general.internal.server.exception";
    private static final String ERROR_SERVICE_TIMEOUT = "PPA.general.service.timeout.exception";
    private static final String ERROR_JSP_TITLE = "PPA_ES_T_005";
    private static final Locale LOCALE_EN = Locale.ENGLISH;
    protected static final Locale LOCALE_FA = new Locale("fa");

    private final MessageSource messageSource;


    public ReactiveExceptionHandler(ErrorAttributes errorAttributes,
                                    WebProperties.Resources resources,
                                    ApplicationContext applicationContext,
                                    MessageSource messageSource) {

        super(errorAttributes, resources, applicationContext);
        this.messageSource = messageSource;
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {

        Throwable error = getError(request);
        log.error("An error has been occurred", error);
        ApiError apiError;

        switch (error.getClass().getSimpleName()) {
            case "GeneralException":
                BaseException ex = (BaseException) error;
                apiError = ApiError.builder()
                        .status(ex.getHttpStatusCode())
                        .message(initializeMessage(ex.getCode(), LOCALE_EN))
                        .localizedMessage(initializeMessage(ex.getCode(), LOCALE_FA))
                        .code(ex.getCode())
                        .extraData(ex.getExtraData())
                        .build();
                break;


            case "ValidationException":
                BaseException ex2 = (BaseException) error;
                List<ApiSubError> subErrorList = new ArrayList<>();

                if (ex2.getMessage() != null) {
                    ApiSubError subError = new ApiValidationError(
                            null,
                            null,
                            "E" + HttpStatus.BAD_REQUEST.value() + "NOTC",
                            initializeMessage(ex2.getMessage(), LOCALE_EN),
                            initializeMessage(ex2.getMessage().substring(ex2.getMessage().indexOf(":") + 1).trim(), LOCALE_FA)
                    );
                    subErrorList.add(subError);
                }
                apiError = ApiError.builder()
                        .status(ex2.getHttpStatusCode())
                        .message(initializeMessage(ex2.getCode(), LOCALE_EN))
                        .localizedMessage(initializeMessage(ex2.getCode(), LOCALE_FA))
                        .code(ex2.getCode())
                        .extraData(ex2.getExtraData())
                        .subErrors(subErrorList)
                        .build();
                break;


            case "ServerWebInputException":
                apiError = ApiError.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(initializeMessage("PPA.general.validator.EBP40000001", LOCALE_EN))
                        .localizedMessage(initializeMessage("PPA.general.validator.EBP40000001", LOCALE_FA))
                        .code("E" + HttpStatus.BAD_REQUEST.value() + "NOTC")
                        .build();

                break;


            case "WebExchangeBindException":
                List<ApiSubError> subErrorList2 = new ArrayList<>();
                List<FieldError> errors = ((WebExchangeBindException) error).getBindingResult().getFieldErrors();

                for (FieldError fieldError : errors) {
                    ApiSubError subError = new ApiValidationError(
                            null,
                            null,
                            fieldError.getCode(),
                            initializeMessage(fieldError.getDefaultMessage(), LOCALE_EN),
                            initializeMessage(fieldError.getDefaultMessage(), LOCALE_FA)
                    );
                    subErrorList2.add(subError);
                }
                apiError = ApiError.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message(initializeMessage(ERROR_METHOD_ARGUMENT_NOT_VALID, LOCALE_EN))
                        .localizedMessage(initializeMessage(ERROR_METHOD_ARGUMENT_NOT_VALID, LOCALE_FA))
                        .code("E" + HttpStatus.BAD_REQUEST.value() + "NOTC")
                        .subErrors(subErrorList2)
                        .build();

                break;

            case "MongoNotPrimaryException":
                apiError = ApiError.builder()
                        .status(HttpStatus.NOT_ACCEPTABLE)
                        .message(initializeMessage(ERROR_GENERAL_DB_EXCEPTION, LOCALE_EN))
                        .localizedMessage(initializeMessage(ERROR_GENERAL_DB_EXCEPTION, LOCALE_FA))
                        .code("E" + HttpStatus.NOT_ACCEPTABLE.value() + "NOTC")
                        .build();

                break;


            default:
                apiError = ApiError.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .message(initializeMessage(ERROR_INTERNAL_SERVER, LOCALE_EN))
                        .localizedMessage(initializeMessage(ERROR_INTERNAL_SERVER, LOCALE_FA))
                        .code(ERROR_INTERNAL_SERVER)
                        .build();
        }

        return ServerResponse
                .status(apiError.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(apiError));
    }


    protected String initializeMessage(String errorCode, Locale locale) {

        return messageSource.getMessage(errorCode, null, locale);
    }

    private List<BaseException> getSubExceptions(Map<String, Object> messageParameters) {
        if (messageParameters.containsKey("exceptions")) {
            return (List<BaseException>) messageParameters.get("exceptions");
        }
        return Collections.emptyList();
    }

    protected ResponseEntity<Object> buildResponseEntity(ApiError apiError, Throwable ex) {
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}
