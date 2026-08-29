package com.urlshortener.admin;

import java.util.List;

public record AdminSummaryResponse(
        long totalUsers,
        long totalLinks,
        long activeLinks,
        long disabledLinks,
        long deletedLinks,
        List<AdminUserResponse> recentUsers,
        List<AdminLinkResponse> recentLinks
) { }
