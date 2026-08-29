package com.urlshortener.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
        @NotBlank(message = "longUrl is required")
        @Size(max = 2048, message = "longUrl must be at most 2048 characters")
        String longUrl,
        @Size(max = 32, message = "alias must be at most 32 characters")
        String alias
) {

        public String longUrl() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'longUrl'");
        } }
