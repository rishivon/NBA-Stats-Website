package com.example.nbavisualizer.model.dashboard;

public record TeamSummary(
        Integer id,
        String fullName,
        String abbreviation,
        String city,
        String name,
        String conference,
        String division,
        String logoPath,
        Integer season,
        Integer wins,
        Integer losses,
        Double winPercentage,
        Integer conferenceRank,
        String conferenceRankDisplay,
        String recordDisplay,
        String summary
) {
}
