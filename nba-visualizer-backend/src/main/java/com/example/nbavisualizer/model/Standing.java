package com.example.nbavisualizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Standing {
    private Integer teamId;
    private String teamName;
    private String teamAbbr;
    private String conference;
    private Integer wins;
    private Integer losses;
    private Double winPercentage;
    private Integer lastTenWins;
    private Integer lastTenLosses;
    private Integer winStreak;

    public String getWinPercentageFormatted() {
        return String.format("%.3f", winPercentage).substring(1);
    }

    public String getLastTenRecord() {
        return lastTenWins + "-" + lastTenLosses;
    }

    public String getStreakDisplay() {
        String prefix = winStreak >= 0 ? "W" : "L";
        return prefix + Math.abs(winStreak);
    }
}
