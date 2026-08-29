package com.urlshortener.admin;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String email,
        String displayName,
        String role,
        Instant createdAt,
        long linkCount
) { }
