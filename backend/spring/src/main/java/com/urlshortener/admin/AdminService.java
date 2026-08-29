package com.urlshortener.admin;

import com.urlshortener.auth.User;
import com.urlshortener.auth.UserRepository;
import com.urlshortener.url.ShortUrl;
import com.urlshortener.url.ShortUrlRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final ShortUrlRepository urlRepository;
    private final String baseUrl;

    public AdminService(UserRepository userRepository,
                        ShortUrlRepository urlRepository,
                        @Value("${app.base-url}") String baseUrl) {
        this.userRepository = userRepository;
        this.urlRepository = urlRepository;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        List<ShortUrl> links = urlRepository.findAll();
        return new AdminSummaryResponse(
                userRepository.count(),
                links.size(),
                links.stream().filter(link -> link.isActive() && !link.isDeleted()).count(),
                links.stream().filter(link -> !link.isActive() && !link.isDeleted()).count(),
                links.stream().filter(ShortUrl::isDeleted).count(),
                users(PageRequest.of(0, 5)),
                links(PageRequest.of(0, 5))
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users() {
        return users(PageRequest.of(0, 100));
    }

    @Transactional(readOnly = true)
    public List<AdminLinkResponse> links() {
        return links(PageRequest.of(0, 100));
    }

    private List<AdminUserResponse> users(PageRequest pageRequest) {
        return userRepository.findAll(pageRequest).stream()
                .map(user -> new AdminUserResponse(
                        user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
                        user.getCreatedAt(), urlRepository.countByOwner(user)))
                .toList();
    }

    private List<AdminLinkResponse> links(PageRequest pageRequest) {
        return urlRepository.findAll(pageRequest).stream()
                .map(this::toResponse)
                .toList();
    }

    private AdminLinkResponse toResponse(ShortUrl link) {
        return new AdminLinkResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getLongUrl(),
                link.getCustomAlias(),
                link.getOwner() == null ? "Anonymous" : link.getOwner().getEmail(),
                link.getCreatedAt(),
                link.getUpdatedAt(),
                link.isActive(),
                link.isDeleted()
        );
    }
}
