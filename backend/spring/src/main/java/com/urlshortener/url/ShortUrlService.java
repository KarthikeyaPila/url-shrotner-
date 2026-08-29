package com.urlshortener.url;

import com.urlshortener.auth.User;

import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
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
        return create(request, null);
    }

    public ShortUrlResponse create(CreateShortUrlRequest request, User owner) {
        String longUrl = request.longUrl().trim();
        validateUrl(longUrl);

        if (request.alias() != null && !request.alias().isBlank()) {
            return createWithAlias(longUrl, request.alias().trim(), owner);
        }

        ShortUrl saved = repository.save(new ShortUrl(longUrl, generateUniqueCode(), null, owner));
        return toResponse(saved);
    }

    public String resolve(String code) {
        return repository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new UrlNotFoundException(code)).getLongUrl();
    }

    public List<ShortUrlResponse> listMine(User owner) {
        return repository.findAllByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(String code, User owner) {
        ShortUrl shortUrl = repository.findByCodeAndOwner(code, owner)
                .orElseThrow(() -> new UrlNotFoundException(code));
        if (shortUrl.isDeleted()) throw new UrlNotFoundException(code);
        shortUrl.delete();
        repository.save(shortUrl);
    }

    public void setActive(String code, boolean active, User owner) {
        ShortUrl shortUrl = repository.findByCodeAndOwner(code, owner)
                .orElseThrow(() -> new UrlNotFoundException(code));
        if (shortUrl.isDeleted()) throw new UrlNotFoundException(code);
        shortUrl.setActive(active);
        repository.save(shortUrl);
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

    private ShortUrlResponse createWithAlias(String longUrl, String alias, User owner) {
        if (!alias.matches("[A-Za-z0-9_-]+")) {
            throw new InvalidAliasException();
        }

        Optional<ShortUrl> existing = repository.findByCode(alias);
        if (existing.isPresent()) {
            if (existing.get().getLongUrl().equals(longUrl)) {
                if (existing.get().isDeleted()
                        && owner != null
                        && existing.get().getOwner() != null
                        && existing.get().getOwner().getId().equals(owner.getId())) {
                    existing.get().restore(longUrl);
                    return toResponse(repository.save(existing.get()));
                }
                if (!existing.get().isDeleted()) return toResponse(existing.get());
            }
            if (existing.get().isDeleted()
                    && owner != null
                    && existing.get().getOwner() != null
                    && existing.get().getOwner().getId().equals(owner.getId())) {
                existing.get().restore(longUrl);
                return toResponse(repository.save(existing.get()));
            }
            throw new AliasAlreadyExistsException(alias);
        }

        return toResponse(repository.save(new ShortUrl(longUrl, alias, alias, owner)));
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
        return new ShortUrlResponse(
                shortUrl.getCode(),
                baseUrl + "/" + shortUrl.getCode(),
                shortUrl.getLongUrl(),
                shortUrl.getCustomAlias(),
                shortUrl.getCreatedAt(),
                shortUrl.getUpdatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getDisabledAt(),
                shortUrl.isActive(),
                shortUrl.isDeleted()
        );
    }
}
