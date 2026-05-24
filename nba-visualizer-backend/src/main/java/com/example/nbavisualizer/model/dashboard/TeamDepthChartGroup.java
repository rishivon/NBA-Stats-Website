package com.example.nbavisualizer.model.dashboard;

import java.util.List;

public record TeamDepthChartGroup(
        String position,
        List<TeamDepthChartPlayer> players
) {
}
