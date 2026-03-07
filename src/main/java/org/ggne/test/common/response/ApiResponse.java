package org.ggne.test.common.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final int status;               // HTTP 상태 코드 추가
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(boolean success, int status, T data, ErrorResponse error) {
        this.success = success;
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(int status, T data) {
        return new ApiResponse<>(true, status, data, null);
    }

    public static <T> ApiResponse<T> error(int status, String code, String message) {
        return new ApiResponse<>(false, status, null, new ErrorResponse(code, message));
    }

    @Getter
    public static class ErrorResponse {
        private final String code;
        private final String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
