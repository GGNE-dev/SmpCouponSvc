package org.ggne.test.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCreateRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String name;

    public UserCreateRequest(String email, String name) {
        this.email = email;
        this.name = name;
    }
}
