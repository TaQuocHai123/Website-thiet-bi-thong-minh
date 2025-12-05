package com.project.DuAnTotNghiep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Order(1)
public class AdminSecurityConfig {

    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .antMatcher("/admin/**")
                .authorizeRequests()
                .antMatchers("/admin-login", "/admin/login", "/admin/vendors/**", "/admin/assets/**").permitAll()
                .antMatchers("/admin/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .and()
                .formLogin()
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/admin_login")
                .usernameParameter("email")
                .defaultSuccessUrl("/admin")
                .and()
                .logout()
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login");

        return http.build();
    }
}