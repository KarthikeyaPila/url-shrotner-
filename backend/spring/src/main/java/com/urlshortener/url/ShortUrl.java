package com.urlshortener.url;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "short_urls", uniqueConstraints = @UniqueConstraint(name = "uk_short_urls_code", columnNames = "code"))
public class ShortUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String longUrl;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ShortUrl() { }

    public ShortUrl(String longUrl, String code) {
        this.longUrl = longUrl;
        this.code = code;
        this.createdAt = Instant.now();
    }

    public String getLongUrl() { return longUrl; }
    public String getCode() { return code; }
    public Instant getCreatedAt() { return createdAt; }
}
