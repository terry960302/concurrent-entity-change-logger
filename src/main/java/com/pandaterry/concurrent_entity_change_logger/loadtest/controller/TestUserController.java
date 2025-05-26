package com.pandaterry.concurrent_entity_change_logger.loadtest.controller;

import com.pandaterry.concurrent_entity_change_logger.loadtest.dto.UserDto;
import com.pandaterry.concurrent_entity_change_logger.loadtest.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/users")
@RequiredArgsConstructor
public class TestUserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.createUser(userDto));
    }

    @PutMapping("/{id}/login")
    public ResponseEntity<UserDto> updateLogin(@PathVariable Long id) {
        return ResponseEntity.ok(userService.updateLogin(id));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<UserDto> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<UserDto> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }
}