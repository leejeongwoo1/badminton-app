package io.github.leejeongwoo1.badminton.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전체에서 쓰는 에러 목록. API 명세의 에러 코드와 1:1로 맞춘다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않아요."),

    // 회원
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일이에요."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임이에요.");

    private final HttpStatus status;
    private final String message;
}
