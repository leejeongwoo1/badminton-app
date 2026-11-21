package io.github.leejeongwoo1.badminton.user.dto;

import io.github.leejeongwoo1.badminton.user.Grade;
import io.github.leejeongwoo1.badminton.user.User;

/**
 * 응답에는 엔티티를 그대로 내보내지 않는다 (비밀번호 해시 같은 내부 정보가 새지 않게)
 */
public record SignupResponse(Long userId, String nickname, Grade grade, int mmr) {

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getNickname(), user.getGrade(), user.getMmr());
    }
}
