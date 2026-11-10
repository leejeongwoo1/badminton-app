package io.github.leejeongwoo1.badminton.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 가입 시 사용자가 고르는 급수. 초기 MMR을 정하는 데만 쓰고, 이후 매칭은 MMR로만 한다.
 */
@Getter
@RequiredArgsConstructor
public enum Grade {
    BEGINNER(800),
    D(1000),
    C(1200),
    B(1400),
    A(1600);

    private final int initialMmr;
}
