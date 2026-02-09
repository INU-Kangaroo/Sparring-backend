package com.kangaroo.sparring.domain.user.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.domain.user.type.SocialProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 10)
    private SocialProvider provider; // "google", "kakao", "local"

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 비즈니스 메서드
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateProfile(String username, String profileImageUrl) {
        this.username = username;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateUsername(String username) {
        if (username != null && !username.isBlank()) {
            this.username = username;
        }
    }

    public void updateEmail(String email) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
    }

    public void updateBirthDate(LocalDate birthDate) {
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
    }

    public void updateGender(Gender gender) {
        if (gender != null) {
            this.gender = gender;
        }
    }

    public void updatePassword(String encodedPassword) {
        if (encodedPassword != null && !encodedPassword.isBlank()) {
            this.password = encodedPassword;
        }
    }

    public void updateProvider(SocialProvider provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    public void deactivate() {
        this.isActive = false;
        this.delete();
    }

    // 소셜 로그인 사용자 여부
    public boolean isSocialUser() {
        return this.provider == SocialProvider.GOOGLE || this.provider == SocialProvider.KAKAO;
    }

    public boolean isLocalUser() {
        return this.provider == null || this.provider == SocialProvider.LOCAL;
    }
}
