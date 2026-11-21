package io.github.leejeongwoo1.badminton.global.error;

/**
 * 에러 응답 형식: { "code": "DUPLICATE_EMAIL", "message": "이미 가입된 이메일이에요." }
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }
}
