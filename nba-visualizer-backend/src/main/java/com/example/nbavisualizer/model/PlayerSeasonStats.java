package com.example.nbavisualizer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "player_season_stats")
public class PlayerSeasonStats {
    @Id
    private Integer playerId;
    private Double pts;
    private Double reb;
    private Double ast;
    private Double stl;
    private Double blk;
    private Double fgPct;
    private Double threePointPct;
    private Instant lastUpdated;
}
