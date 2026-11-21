package io.github.leejeongwoo1.badminton.user.dto;

import io.github.leejeongwoo1.badminton.user.Gender;
import io.github.leejeongwoo1.badminton.user.Grade;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(min = 2, max = 30) String nickname,
        @NotNull Gender gender,
        @NotNull Grade grade
) {
}
