package com.kangaroo.sparring.domain.user.repository;

import com.kangaroo.sparring.domain.user.entity.SocialProvider;
import com.kangaroo.sparring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(SocialProvider provider, String socialId);

    boolean existsByEmail(String email);
}