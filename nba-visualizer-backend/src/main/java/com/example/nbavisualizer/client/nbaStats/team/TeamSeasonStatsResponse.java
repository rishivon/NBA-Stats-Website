package com.example.nbavisualizer.client.nbaStats.team;

import lombok.Data;

@Data
public class TeamSeasonStatsResponse {
    private Integer teamId;
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
}
