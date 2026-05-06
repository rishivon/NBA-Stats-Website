package com.example.nbavisualizer.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nba.refresh")
public record NbaRefreshProperties(
        int standingsStaleMinutes,
        int metadataStaleDays,
        int jitterMinSeconds,
        int jitterMaxSeconds
) {
}
