package com.tanwb.navigation.config;

import com.tanwb.navigation.mapper.SysUserMapper;
import com.tanwb.navigation.security.BCryptCredentialsMatcher;
import com.tanwb.navigation.security.UserRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Base64;

@Configuration
public class ShiroConfig {

    @Value("${app.shiro.remember-me-cipher-key:Z3VpZGUtd2Vic2l0ZS1uYXZpZ2F0aW9u}")
    private String rememberMeCipherKey;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Realm userRealm(SysUserMapper sysUserMapper, BCryptPasswordEncoder passwordEncoder) {
        UserRealm realm = new UserRealm(sysUserMapper);
        realm.setCredentialsMatcher(new BCryptCredentialsMatcher(passwordEncoder));
        return realm;
    }

    @Bean
    public SimpleCookie rememberMeCookie() {
        SimpleCookie cookie = new SimpleCookie("rememberMe");
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30天
        cookie.setHttpOnly(true);
        return cookie;
    }

    @Bean
    public CookieRememberMeManager rememberMeManager(SimpleCookie rememberMeCookie) {
        CookieRememberMeManager rememberMeManager = new CookieRememberMeManager();
        byte[] key = Base64.getDecoder().decode(rememberMeCipherKey);
        rememberMeManager.setCipherKey(key);
        rememberMeManager.setCookie(rememberMeCookie);
        return rememberMeManager;
    }

    @Bean
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setGlobalSessionTimeout(7 * 24 * 60 * 60 * 1000L); // 7天滑动过期
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        sessionManager.setSessionValidationSchedulerEnabled(true);
        return sessionManager;
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();
        chainDefinition.addPathDefinition("/api/**", "anon");
        return chainDefinition;
    }
}
