package com.example.nbavisualizer.client.nbaStats;

import lombok.Data;
import java.util.List;

@Data
public class StandingsResponseWrapper {
    private List<StandingResponse> data;
    private Integer count;
    private String status;
}