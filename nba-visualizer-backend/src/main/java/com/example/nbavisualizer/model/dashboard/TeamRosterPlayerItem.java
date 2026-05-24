package com.example.nbavisualizer.model.dashboard;

public record TeamRosterPlayerItem(
        Integer playerId,
        String fullName,
        String position,
        String jersey,
        String height,
        String weight,
        String salary
) {
}
