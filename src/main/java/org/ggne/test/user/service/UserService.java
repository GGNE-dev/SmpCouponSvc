package org.ggne.test.user.service;

import lombok.RequiredArgsConstructor;
import org.ggne.test.common.exception.BusinessException;
import org.ggne.test.common.exception.ErrorCode;
import org.ggne.test.user.domain.User;
import org.ggne.test.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public Long signup(String email, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATED);
        }
        User user = new User(email, name);
        return userRepository.save(user).getId();
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
