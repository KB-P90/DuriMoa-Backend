package com.project.common.config;

import com.project.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .headers().frameOptions().disable()
            .and()
            .authorizeRequests()
                .antMatchers(
                        "/api/auth/signup",
                        "/api/auth/login",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/webjars/**")
                    .permitAll()
                .antMatchers(
                        HttpMethod.GET,
                        "/api/goal/stat",
                        "/api/goal/stat/estimate")
                    .permitAll()
                .antMatchers("/api/goal", "/api/goal/**")
                    .authenticated()
                // 로그인해야 볼 수 있는 API는 아래처럼 이 위치에 추가하세요.
                // 예: .antMatchers("/api/members/**").authenticated()
                // 로그인 없이 볼 수 있는 API는 위의 permitAll() 목록에 추가하세요.
                .anyRequest().permitAll()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
