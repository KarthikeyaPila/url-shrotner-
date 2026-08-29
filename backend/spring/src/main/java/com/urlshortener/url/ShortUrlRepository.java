package com.urlshortener.url;

import com.urlshortener.auth.User;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    boolean existsByCode(String code);
    Optional<ShortUrl> findByCode(String code);
    Optional<ShortUrl> findByCodeAndActiveTrue(String code);
    List<ShortUrl> findAllByOwnerOrderByCreatedAtDesc(User owner);
    Optional<ShortUrl> findByCodeAndOwner(String code, User owner);
    long countByOwner(User owner);
}
