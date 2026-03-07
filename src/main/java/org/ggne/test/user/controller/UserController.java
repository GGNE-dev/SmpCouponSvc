package org.ggne.test.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ggne.test.common.response.ApiResponse;
import org.ggne.test.user.domain.User;
import org.ggne.test.user.dto.UserCreateRequest;
import org.ggne.test.user.dto.UserResponse;
import org.ggne.test.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signup(@RequestBody @Valid UserCreateRequest request) {
        Long userId = userService.signup(request.getEmail(), request.getName());
        User user = userService.getUser(userId);
        return ApiResponse.success(HttpStatus.CREATED.value(), UserResponse.from(user));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        User user = userService.getUser(userId);
        return ApiResponse.success(HttpStatus.OK.value(), UserResponse.from(user));
    }
}
