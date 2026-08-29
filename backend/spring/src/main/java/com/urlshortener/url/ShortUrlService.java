package com.urlshortener.url;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShortUrlService {
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int GENERATED_LENGTH = 7;
    private final ShortUrlRepository repository;
    private final String baseUrl;

    public ShortUrlService(ShortUrlRepository repository, @Value("${app.base-url}") String baseUrl) {
        this.repository = repository;
        this.baseUrl = baseUrl.replaceAll("/$", ""); //this represents the very end of a string.
    }

    public ShortUrlResponse create(CreateShortUrlRequest request) {
        String longUrl = request.longUrl().trim();
        validateUrl(longUrl);

        if (request.alias() != null && !request.alias().isBlank()) {
            return createWithAlias(longUrl, request.alias().trim());
        }

        ShortUrl saved = repository.save(new ShortUrl(longUrl, generateUniqueCode()));
        return toResponse(saved);
    }

    public String resolve(String code) {
        return repository.findByCode(code).orElseThrow(() -> new UrlNotFoundException(code)).getLongUrl();
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder result = new StringBuilder(GENERATED_LENGTH);
            for (int i = 0; i < GENERATED_LENGTH; i++) {
                result.append(BASE62.charAt(RANDOM.nextInt(BASE62.length())));
            }
            code = result.toString();
        } while (repository.existsByCode(code));
        return code;
    }

    private ShortUrlResponse createWithAlias(String longUrl, String alias) {
        if (!alias.matches("[A-Za-z0-9_-]+")) {
            throw new InvalidAliasException();
        }

        Optional<ShortUrl> existing = repository.findByCode(alias);
        if (existing.isPresent()) {
            if (existing.get().getLongUrl().equals(longUrl)) {
                return toResponse(existing.get());
            }
            throw new AliasAlreadyExistsException(alias);
        }

        return toResponse(repository.save(new ShortUrl(longUrl, alias)));
    }

    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new InvalidUrlException();
        }
    }

    private ShortUrlResponse toResponse(ShortUrl shortUrl) {
        return new ShortUrlResponse(shortUrl.getCode(), baseUrl + "/" + shortUrl.getCode(), shortUrl.getLongUrl(), shortUrl.getCreatedAt());
    }
}
