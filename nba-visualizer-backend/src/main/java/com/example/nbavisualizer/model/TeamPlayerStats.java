package com.example.nbavisualizer.model;

import com.example.nbavisualizer.model.id.TeamPlayerSeasonId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
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
@Table(name = "team_player_stats")
@IdClass(TeamPlayerSeasonId.class)
public class TeamPlayerStats {
    @Id
    private Integer teamId;
    @Id
    private Integer season;
    @Id
    private Integer playerId;
    private String playerName;
    private Double pts;
    private Double reb;
    private Double ast;
    private Double stl;
    private Double blk;
    private Double plusMinus;
    private Instant lastUpdated;
}
