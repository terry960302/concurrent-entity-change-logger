package com.pandaterry.concurrent_entity_change_logger.loadtest.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_user")
@Getter
@Setter
@NoArgsConstructor
public class TestUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String phoneNumber;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private boolean active;

    @Column
    private int loginCount;

    @Builder
    public TestUser(String name, String email, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.active = true;
        this.loginCount = 0;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.loginCount++;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}