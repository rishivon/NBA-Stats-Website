package com.example.nbavisualizer.client.nbaStats.team;

import lombok.Data;

@Data
public class TeamRosterPlayerResponse {
    private Integer playerId;
    private Integer teamId;
    private Integer season;
    private String fullName;
    private String firstName;
    private String lastName;
    private String position;
    private String jersey;
    private String height;
    private String weight;
    private String salary;
}
