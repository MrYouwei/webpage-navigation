package com.tanwb.navigation.security;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptCredentialsMatcher implements CredentialsMatcher {
    private final BCryptPasswordEncoder passwordEncoder;

    public BCryptCredentialsMatcher(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        if (!(token instanceof UsernamePasswordToken)) {
            return false;
        }
        UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
        Object credentials = info.getCredentials();
        if (credentials == null) {
            return false;
        }
        String rawPassword = new String(usernamePasswordToken.getPassword());
        return passwordEncoder.matches(rawPassword, credentials.toString());
    }
}
