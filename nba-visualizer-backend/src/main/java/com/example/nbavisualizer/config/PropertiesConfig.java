package com.example.nbavisualizer.config;

import com.example.nbavisualizer.service.NbaRefreshProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NbaRefreshProperties.class)
public class PropertiesConfig {
}
