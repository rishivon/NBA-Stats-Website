package com.example.nbavisualizer.client.nbaStats.team;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TeamGameLogResponse {
    @JsonProperty("Game_ID")
    @JsonAlias("GAME_ID")
    private String gameId;
    @JsonProperty("GAME_DATE")
    private String gameDate;
    @JsonProperty("MATCHUP")
    private String matchup;
    @JsonProperty("WL")
    private String winLoss;
    @JsonProperty("W")
    private Integer wins;
    @JsonProperty("L")
    private Integer losses;
    @JsonProperty("PTS")
    private Integer points;
    @JsonProperty("PLUS_MINUS")
    private Double plusMinus;
}
