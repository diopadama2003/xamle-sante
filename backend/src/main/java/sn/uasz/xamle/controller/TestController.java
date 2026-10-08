package sn.uasz.xamle.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class TestController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of("email", auth.getName(), "roles", auth.getAuthorities().toString());
    }

    @GetMapping("/admin/ping")
    public Map<String, String> adminPing() {
        return Map.of("message", "Bienvenue admin");
    }
}