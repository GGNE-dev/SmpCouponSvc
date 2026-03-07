package org.ggne.test.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ggne.test.user.domain.User;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String email;
    private String name;

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
