package com.tanwb.navigation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tanwb.navigation.dto.AuthUserResponse;
import com.tanwb.navigation.dto.LoginRequest;
import com.tanwb.navigation.dto.RegisterRequest;
import com.tanwb.navigation.entity.SysUser;
import com.tanwb.navigation.exception.ApiException;
import com.tanwb.navigation.mapper.SysUserMapper;
import com.tanwb.navigation.security.AuthPrincipal;
import java.util.Map;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements AuthOperations {
    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final NavDataOperations navDataService;
    private final SecurityService securityService;

    public AuthService(
            SysUserMapper sysUserMapper,
            BCryptPasswordEncoder passwordEncoder,
            NavDataOperations navDataService,
            SecurityService securityService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.navDataService = navDataService;
        this.securityService = securityService;
    }

    @Transactional
    @Override
    public AuthUserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "两次输入的密码不一致");
        }
        if (existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        sysUserMapper.insert(user);

        loginSubject(username, request.getPassword());
        Map<String, Object> navData = navDataService.getOrCreateData(user.getId());
        return new AuthUserResponse(user.getId(), user.getUsername(), navData);
    }

    @Override
    public AuthUserResponse login(LoginRequest request) {
        loginSubject(request.getUsername().trim(), request.getPassword());
        AuthPrincipal principal = securityService.requireUser();
        Map<String, Object> navData = navDataService.getOrCreateData(principal.getUserId());
        return new AuthUserResponse(principal.getUserId(), principal.getUsername(), navData);
    }

    @Override
    public AuthUserResponse currentUser() {
        AuthPrincipal principal = securityService.requireUser();
        Map<String, Object> navData = navDataService.getOrCreateData(principal.getUserId());
        return new AuthUserResponse(principal.getUserId(), principal.getUsername(), navData);
    }

    @Override
    public void logout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject != null) {
            subject.logout();
        }
    }

    private void loginSubject(String username, String password) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            subject.logout();
        }
        subject.login(new UsernamePasswordToken(username, password));
    }

    private boolean existsByUsername(String username) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        return count != null && count > 0;
    }
}
