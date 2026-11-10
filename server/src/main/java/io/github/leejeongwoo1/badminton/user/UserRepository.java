package io.github.leejeongwoo1.badminton.user;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 인터페이스만 선언하면 Spring Data JPA가 구현체를 만들어준다.
 * 메서드 이름(existsByEmail)만 보고 SQL을 자동 생성한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
