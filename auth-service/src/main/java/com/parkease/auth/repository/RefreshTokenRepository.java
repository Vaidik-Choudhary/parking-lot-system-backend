package com.parkease.auth.repository;

import com.parkease.auth.entity.RefreshToken;
import com.parkease.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByUser(User user);

    @Modifying
    int deleteByUser(User user);
}
