package com.example.nbavisualizer.model;

import com.example.nbavisualizer.model.id.TeamSeasonId;
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
@Table(name = "team_season_stats")
@IdClass(TeamSeasonId.class)
public class TeamSeasonStats {
    @Id
    private Integer teamId;
    @Id
    private Integer season;
    private Double pts;
    private Double reb;
    private Double ast;
    private Double stl;
    private Double blk;
    private Double plusMinus;
    private Integer ptsRank;
    private Integer rebRank;
    private Integer astRank;
    private Integer stlRank;
    private Integer blkRank;
    private Integer plusMinusRank;
    private Instant lastUpdated;
}
