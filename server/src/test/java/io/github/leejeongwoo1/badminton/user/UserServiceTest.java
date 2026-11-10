package io.github.leejeongwoo1.badminton.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.leejeongwoo1.badminton.global.error.BusinessException;
import io.github.leejeongwoo1.badminton.global.error.ErrorCode;
import io.github.leejeongwoo1.badminton.user.dto.SignupRequest;
import io.github.leejeongwoo1.badminton.user.dto.SignupResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 DB(docker compose의 PostgreSQL)에 붙여서 테스트한다.
 * @Transactional: 테스트가 끝나면 넣은 데이터를 롤백해서 DB를 깨끗하게 유지한다.
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Test
    void 회원가입하면_급수에_맞는_초기_MMR이_정해지고_비밀번호는_암호화된다() {
        SignupResponse response = userService.signup(
                new SignupRequest("a@test.com", "password123", "정우", Gender.MALE, Grade.C));

        assertThat(response.mmr()).isEqualTo(1200);

        User saved = userRepository.findById(response.userId()).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
    }

    @Test
    void 같은_이메일로_가입하면_409_DUPLICATE_EMAIL() {
        userService.signup(new SignupRequest("dup@test.com", "password123", "닉네임1", Gender.MALE, Grade.D));

        assertThatThrownBy(() -> userService.signup(
                new SignupRequest("dup@test.com", "password123", "닉네임2", Gender.FEMALE, Grade.B)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }
}
