package com.example.nbavisualizer.model.dashboard;

public record TeamStatCard(
        String key,
        String label,
        Double value,
        String formattedValue,
        Integer rank,
        String rankDisplay,
        boolean highlighted
) {
}
