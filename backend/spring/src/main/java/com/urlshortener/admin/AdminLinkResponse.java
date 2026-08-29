package com.urlshortener.admin;

import java.time.Instant;

public record AdminLinkResponse(
        String code,
        String shortUrl,
        String longUrl,
        String customAlias,
        String ownerEmail,
        Instant createdAt,
        Instant updatedAt,
        boolean active,
        boolean deleted
) { }
