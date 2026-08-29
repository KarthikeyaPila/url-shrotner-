package com.urlshortener.url;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    boolean existsByCode(String code);
    Optional<ShortUrl> findByCode(String code);
}
