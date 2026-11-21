package io.github.leejeongwoo1.badminton.user;

import io.github.leejeongwoo1.badminton.global.error.BusinessException;
import io.github.leejeongwoo1.badminton.global.error.ErrorCode;
import io.github.leejeongwoo1.badminton.user.dto.SignupRequest;
import io.github.leejeongwoo1.badminton.user.dto.SignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor   // final 필드를 받는 생성자를 만들어줌 → 생성자 주입
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 1차 검사: 친절한 에러 메시지를 위해 미리 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.gender(),
                request.grade()
        );

        // 2차 방어: 위 검사와 저장 사이에 같은 이메일로 동시에 가입하면 둘 다 1차 검사를 통과할 수 있다.
        // 그 경우 DB의 UNIQUE 제약이 두 번째 저장을 막아주고, 여기서 409로 바꾼다.
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        return SignupResponse.from(user);
    }
}
