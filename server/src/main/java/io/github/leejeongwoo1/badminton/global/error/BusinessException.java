package io.github.leejeongwoo1.badminton.global.error;

import lombok.Getter;

/**
 * 규칙 위반(중복 가입, 상태 위반 등)을 나타내는 예외.
 * 서비스 계층에서 던지면 GlobalExceptionHandler가 HTTP 응답으로 바꿔준다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
