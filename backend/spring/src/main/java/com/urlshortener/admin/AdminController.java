package com.urlshortener.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public AdminSummaryResponse summary() {
        return service.summary();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return service.users();
    }

    @GetMapping("/urls")
    public List<AdminLinkResponse> links() {
        return service.links();
    }
}
