package com.tanwb.navigation.service;

import com.tanwb.navigation.exception.ApiException;
import com.tanwb.navigation.security.AuthPrincipal;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    public AuthPrincipal requireUser() {
        Subject subject = SecurityUtils.getSubject();
        if (subject == null || !subject.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Object principal = subject.getPrincipal();
        if (!(principal instanceof AuthPrincipal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return (AuthPrincipal) principal;
    }
}
