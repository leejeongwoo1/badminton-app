package io.github.leejeongwoo1.badminton.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * users 테이블과 연결되는 엔티티 (V1__init_schema.sql 참고)
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA용 기본 생성자. 외부에서 new User()로 만들지 못하게 막는다
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // DB의 IDENTITY 컬럼이 번호를 매김
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)   // DB에 'MALE'처럼 이름으로 저장 (숫자로 저장하면 enum 순서가 바뀔 때 데이터가 꼬임)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @Column(nullable = false)
    private int mmr;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 회원가입용 생성 메서드: 초기 MMR은 급수로 정한다 */
    public static User create(String email, String passwordHash, String nickname, Gender gender, Grade grade) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.gender = gender;
        user.grade = grade;
        user.mmr = grade.getInitialMmr();
        user.createdAt = Instant.now();
        return user;
    }
}
