package com.datacent.agent.service;

import com.datacent.agent.dto.AuthResponse;
import com.datacent.agent.dto.LoginRequest;
import com.datacent.agent.entity.User;
import com.datacent.agent.repository.UserRepository;
import com.datacent.agent.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("用户账户已禁用");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        log.info("用户登录成功: {}", user.getUsername());

        return new AuthResponse(token, user.getUsername());
    }

    public User register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setRole("USER");

        User savedUser = userRepository.save(user);
        log.info("新用户注册成功: {}", username);

        return savedUser;
    }
}