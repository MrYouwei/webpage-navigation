package com.tanwb.navigation.controller;

import com.tanwb.navigation.common.ApiResponse;
import com.tanwb.navigation.dto.AuthUserResponse;
import com.tanwb.navigation.dto.LoginRequest;
import com.tanwb.navigation.dto.RegisterRequest;
import com.tanwb.navigation.service.AuthOperations;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthOperations authService;

    public AuthController(AuthOperations authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("注册成功", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthUserResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout() {
        authService.logout();
        return ApiResponse.ok("退出登录成功", null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me() {
        return ApiResponse.ok("成功", authService.currentUser());
    }
}
