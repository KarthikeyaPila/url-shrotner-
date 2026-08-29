package com.urlshortener.url;

import java.time.Instant;

public record ShortUrlResponse(
        String code,
        String shortUrl,
        String longUrl,
        String customAlias,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        Instant disabledAt,
        boolean active,
        boolean deleted
) { }
