package com.example.nbavisualizer.client.nbaStats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StandingResponse {
    @JsonProperty("TeamID")
    private Integer teamId;

    @JsonProperty("TeamCity")
    private String teamCity;

    @JsonProperty("TeamName")
    private String teamName;

    @JsonProperty("TeamSlug")
    private String teamSlug;

    @JsonProperty("Conference")
    private String conference;

    @JsonProperty("Division")
    private String division;

    @JsonProperty("ConferenceRecord")
    private String conferenceRecord;

    @JsonProperty("ConferenceRank")
    private Integer conferenceRank;

    @JsonProperty("WINS")
    private Integer wins;

    @JsonProperty("LOSSES")
    private Integer losses;

    @JsonProperty("WinPCT")
    private Double winPercentage;

    @JsonProperty("L10")
    private String last10;

    @JsonProperty("CurrentStreak")
    private Integer currentStreak;

    @JsonProperty("strCurrentStreak")
    private String strCurrentStreak;
}
