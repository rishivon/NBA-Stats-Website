package com.example.nbavisualizer.model.dashboard;

public record TeamLeader(
        String statKey,
        String statLabel,
        String playerName,
        String initials,
        Double value,
        String formattedValue,
        String unit
) {
}
