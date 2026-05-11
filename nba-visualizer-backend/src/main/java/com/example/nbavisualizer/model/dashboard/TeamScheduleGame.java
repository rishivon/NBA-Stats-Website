package com.example.nbavisualizer.model.dashboard;

public record TeamScheduleGame(
        String date,
        String opponentAbbreviation,
        String opponentName,
        String location,
        String result,
        String resultType,
        boolean completed
) {
}
