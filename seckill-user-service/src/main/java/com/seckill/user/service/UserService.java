package com.seckill.user.service;

import com.seckill.common.entity.User;
import com.seckill.common.util.JwtUtil;
import com.seckill.user.dto.LoginRequest;
import com.seckill.user.dto.LoginResponse;
import com.seckill.user.dto.RegisterRequest;
import com.seckill.user.dto.UserInfoResponse;
import com.seckill.user.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public Long register(RegisterRequest request) {
        User existing = userMapper.selectByUsername(request.getUsername());
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .email(request.getEmail())
                .build();
        userMapper.insert(user);
        return user.getId();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId());
        return new LoginResponse(token, user.getId());
    }

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return new UserInfoResponse(user.getUsername(), user.getEmail());
    }
}
