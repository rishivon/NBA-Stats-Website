package com.example.nbavisualizer.client.nbaStats.team;

import lombok.Data;

@Data
public class TeamDepthChartResponse {
    private Integer teamId;
    private String position;
    private Integer depthOrder;
    private String playerName;
    private String status;
}
