package com.pandaterry.concurrent_entity_change_logger.mock_commerce.user.service;

import com.pandaterry.concurrent_entity_change_logger.mock_commerce.user.dto.UserDto;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.user.entity.TestUser;
import com.pandaterry.concurrent_entity_change_logger.mock_commerce.user.repository.TestUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final TestUserRepository userRepository;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        TestUser user = TestUser.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .phoneNumber(userDto.getPhoneNumber())
                .build();
        return convertToDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateLogin(Long id) {
        TestUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.updateLastLogin();
        return convertToDto(userRepository.save(user));
    }

    @Transactional
    public UserDto activateUser(Long id) {
        TestUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.activate();
        return convertToDto(userRepository.save(user));
    }

    @Transactional
    public UserDto deactivateUser(Long id) {
        TestUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.deactivate();
        return convertToDto(userRepository.save(user));
    }

    private UserDto convertToDto(TestUser user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .active(user.isActive())
                .build();
    }
}