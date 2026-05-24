package com.example.nbavisualizer.client.nbaStats.team;

import lombok.Data;

@Data
public class TeamInjuryResponse {
    private Integer teamId;
    private String playerName;
    private String position;
    private String injury;
    private String expectedReturn;
    private String status;
}
