package com.example.nbavisualizer.model.dashboard;

import java.util.List;

public record TeamDashboard(
        TeamSummary summary,
        List<TeamStatCard> stats,
        List<TeamLeader> leaders,
        List<TeamScheduleGame> schedule,
        List<TeamInjuryReportItem> injuries
) {
}
