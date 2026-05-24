package com.example.nbavisualizer.model;

import com.example.nbavisualizer.model.id.TeamGameId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "team_games")
@IdClass(TeamGameId.class)
public class TeamGame {
    @Id
    private Integer teamId;
    @Id
    private Integer season;
    @Id
    private String gameId;
    private LocalDate gameDate;
    private String matchup;
    private String opponentAbbreviation;
    private String opponentName;
    private String location;
    private String resultType;
    private Integer teamScore;
    private Integer opponentScore;
    private String record;
    private boolean completed;
    private Instant lastUpdated;
}
