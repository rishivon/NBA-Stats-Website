package com.example.nbavisualizer.model;

import com.example.nbavisualizer.model.id.StandingId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "standings")
@IdClass(StandingId.class)
public class Standing {
    @Id
    private Integer season;
    @Id
    private Integer teamId;
    private String teamName;
    private String teamAbbr;
    private String conference;
    private String division;
    private Integer wins;
    private Integer losses;
    private Double winPercentage;
    private Integer conferenceRank;
    private Integer lastTenWins;
    private Integer lastTenLosses;
    private Integer winStreak;
    private Instant lastUpdated;

    @Transient
    public String getWinPercentageFormatted() {
        return String.format("%.3f", winPercentage).substring(1);
    }

    @Transient
    public String getLastTenRecord() {
        return lastTenWins + "-" + lastTenLosses;
    }

    @Transient
    public String getStreakDisplay() {
        String prefix = winStreak >= 0 ? "W" : "L";
        return prefix + Math.abs(winStreak);
    }
}
