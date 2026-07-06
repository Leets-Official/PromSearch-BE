package com.promsearch.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.promsearch.global.response.code.BaseCode;
import com.promsearch.global.response.code.SuccessCode;

@JsonPropertyOrder({"success", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("success")
    private final boolean success;

    private final String code;

    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    private ApiResponse(boolean success, String code, String message, T result) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.result = result;
    }

    public static <T> ApiResponse<T> onSuccess(T result) {
        return onSuccess(SuccessCode.OK, result);
    }

    public static <T> ApiResponse<T> onSuccess(BaseCode code, T result) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
    }

    public static <T> ApiResponse<T> onFailure(String code, String message, T result) {
        return new ApiResponse<>(false, code, message, result);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getResult() {
        return result;
    }
}
