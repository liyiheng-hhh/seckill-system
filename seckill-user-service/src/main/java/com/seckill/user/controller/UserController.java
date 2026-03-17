package com.seckill.user.controller;

import com.seckill.user.dto.LoginRequest;
import com.seckill.user.dto.LoginResponse;
import com.seckill.user.dto.RegisterRequest;
import com.seckill.user.dto.RegisterResponse;
import com.seckill.user.dto.UserInfoResponse;
import com.seckill.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户服务", description = "用户注册、登录、获取用户信息")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        Long userId = userService.register(request);
        return new RegisterResponse(userId, "注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @Operation(summary = "获取用户信息", description = "需在 Header 中携带 Authorization: Bearer {token}")
    @GetMapping("/{userId}")
    public UserInfoResponse getUserInfo(@PathVariable Long userId, HttpServletRequest request) {
        Long tokenUserId = (Long) request.getAttribute("userId");
        if (tokenUserId == null || !tokenUserId.equals(userId)) {
            throw new IllegalArgumentException("无权限访问该用户信息");
        }
        return userService.getUserInfo(userId);
    }
}
