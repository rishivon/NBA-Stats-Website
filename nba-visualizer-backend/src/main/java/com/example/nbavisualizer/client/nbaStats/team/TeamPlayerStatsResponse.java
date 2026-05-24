package com.example.nbavisualizer.client.nbaStats.team;

import lombok.Data;

@Data
public class TeamPlayerStatsResponse {
    private Integer playerId;
    private Integer teamId;
    private Integer season;
    private String playerName;
    private Double pts;
    private Double reb;
    private Double ast;
    private Double stl;
    private Double blk;
    private Double plusMinus;
}
