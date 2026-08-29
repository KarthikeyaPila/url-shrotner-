package com.urlshortener.url;

import com.urlshortener.auth.User;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.validation.Valid;

@RestController
public class ShortUrlController {

    private final ShortUrlService service;

    public ShortUrlController(ShortUrlService service) {
        this.service = service;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request,
                                                   Authentication authentication) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(service.create(request, currentUser(authentication)));
    }

    @GetMapping("/api/urls/mine")
    public List<ShortUrlResponse> mine(Authentication authentication) {
        return service.listMine(requiredUser(authentication));
    }

    @DeleteMapping("/api/urls/{code:[A-Za-z0-9_-]+}")
    public ResponseEntity<Void> delete(@PathVariable String code, Authentication authentication) {
        service.delete(code, requiredUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/urls/{code:[A-Za-z0-9_-]+}/status")
    public ResponseEntity<Void> setStatus(@PathVariable String code,
                                          @RequestBody LinkStatusRequest request,
                                          Authentication authentication) {
        service.setActive(code, request.active(), requiredUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code:[A-Za-z0-9_-]+}")
    public RedirectView redirect(@PathVariable String code) {
        return new RedirectView(service.resolve(code));
    }

    private User currentUser(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
    }

    private User requiredUser(Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user;
    }
}
