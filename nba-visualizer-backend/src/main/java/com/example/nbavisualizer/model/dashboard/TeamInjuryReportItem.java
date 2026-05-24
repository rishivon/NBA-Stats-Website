package com.example.nbavisualizer.model.dashboard;

public record TeamInjuryReportItem(
        String playerName,
        String position,
        String injury,
        String expectedReturn,
        String status
) {
}
