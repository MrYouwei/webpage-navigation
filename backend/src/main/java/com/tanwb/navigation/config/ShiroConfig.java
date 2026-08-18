package com.tanwb.navigation.config;

import com.tanwb.navigation.mapper.SysUserMapper;
import com.tanwb.navigation.security.BCryptCredentialsMatcher;
import com.tanwb.navigation.security.UserRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class ShiroConfig {
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
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setGlobalSessionTimeout(2 * 60 * 60 * 1000L);
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        return sessionManager;
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();
        chainDefinition.addPathDefinition("/api/**", "anon");
        return chainDefinition;
    }
}
