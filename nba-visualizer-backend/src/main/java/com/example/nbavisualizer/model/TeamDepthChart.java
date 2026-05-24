package com.example.nbavisualizer.model;

import com.example.nbavisualizer.model.id.TeamDepthChartId;
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
@Table(name = "team_depth_charts")
@IdClass(TeamDepthChartId.class)
public class TeamDepthChart {
    @Id
    private Integer teamId;
    @Id
    private Integer season;
    @Id
    private String position;
    @Id
    private Integer depthOrder;
    private Integer playerId;
    private String playerName;
    private String status;
    private Instant lastUpdated;
}
