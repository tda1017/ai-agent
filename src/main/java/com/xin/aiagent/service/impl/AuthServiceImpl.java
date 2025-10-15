package com.xin.aiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xin.aiagent.entity.User;
import com.xin.aiagent.common.BusinessException;
import com.xin.aiagent.common.ResultCode;
import com.xin.aiagent.mapper.UserMapper;
import com.xin.aiagent.security.JwtTokenProvider;
import com.xin.aiagent.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserMapper userMapper, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String loginAndIssueToken(String username, String rawPassword) {
        User user = loadEnabledUserByUsername(username);
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("roles", java.util.List.of("ROLE_USER"));
        claims.put("permissions", java.util.List.of("chat:use"));
        return jwtTokenProvider.generateToken(user.getUsername(), claims);
    }

    @Override
    public User loadEnabledUserByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getStatus, 1)
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND_OR_DISABLED, "用户不存在或已禁用");
        }
        return user;
    }

    @Override
    public User registerNewUser(String username, String rawPassword, String email) {
        // 唯一性检查：用户名、邮箱
        Long existsByName = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (existsByName != null && existsByName > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS, "用户名已存在");
        }
        if (email != null && !email.isBlank()) {
            Long existsByEmail = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email));
            if (existsByEmail != null && existsByEmail > 0) {
                throw new BusinessException(ResultCode.EMAIL_EXISTS, "邮箱已存在");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setStatus(1);
        // created_at / updated_at 由 MyBatisPlusMetaConfig 处理（若配置）

        int rows = userMapper.insert(user);
        if (rows != 1 || user.getId() == null) {
            throw new BusinessException(ResultCode.REGISTER_FAILED, "注册失败");
        }
        return user;
    }
}
