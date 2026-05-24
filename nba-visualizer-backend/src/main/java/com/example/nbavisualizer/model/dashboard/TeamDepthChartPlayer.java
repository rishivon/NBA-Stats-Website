package com.example.nbavisualizer.model.dashboard;

public record TeamDepthChartPlayer(
        Integer playerId,
        String playerName,
        Integer depthOrder,
        String status
) {
}
