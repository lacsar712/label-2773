package com.example.employee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
@EnableConfigurationProperties(JwtConfig.class)
public class JwtConfig {
    private String secret;
    private Long accessTokenExpire = 7200000L;
    private Long refreshTokenExpire = 604800000L;
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
