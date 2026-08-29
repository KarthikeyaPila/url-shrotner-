package com.urlshortener.url;

import com.urlshortener.auth.User;

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

    @Column(length = 32)
    private String customAlias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @Column
    private Instant expiresAt;

    @Column
    private Instant disabledAt;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Column
    private Instant deletedAt;

    protected ShortUrl() { }

    public ShortUrl(String longUrl, String code) {
        this(longUrl, code, null, null);
    }

    public ShortUrl(String longUrl, String code, String customAlias, User owner) {
        this.longUrl = longUrl;
        this.code = code;
        this.customAlias = customAlias;
        this.owner = owner;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getLongUrl() { return longUrl; }
    public String getCode() { return code; }
    public String getCustomAlias() { return customAlias; }
    public User getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt != null ? updatedAt : createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getDisabledAt() { return disabledAt; }
    public boolean isActive() { return active; }
    public boolean isDeleted() { return deletedAt != null; }

    public void setActive(boolean active) {
        this.active = active;
        this.disabledAt = active ? null : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void delete() {
        this.active = false;
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
    }

    public void restore(String longUrl) {
        this.longUrl = longUrl;
        this.active = true;
        this.disabledAt = null;
        this.deletedAt = null;
        this.updatedAt = Instant.now();
    }
}
