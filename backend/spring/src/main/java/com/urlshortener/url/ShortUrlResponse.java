package com.urlshortener.url;

import java.time.Instant;

public record ShortUrlResponse(String code, String shortUrl, String longUrl, Instant createdAt) { }
