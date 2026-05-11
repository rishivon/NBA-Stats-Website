package com.example.nbavisualizer.model.dashboard;

public record TeamInjuryReportItem(
        String playerName,
        String injury,
        String expectedReturn,
        String status
) {
}
