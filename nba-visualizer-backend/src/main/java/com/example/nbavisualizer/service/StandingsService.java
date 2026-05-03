package com.example.nbavisualizer.service;

import com.example.nbavisualizer.client.nbaStats.NbaStatsClient;
import com.example.nbavisualizer.client.nbaStats.StandingResponse;
import com.example.nbavisualizer.model.Standing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StandingsService {

    private final NbaStatsClient nbaStatsClient;

    public List<Standing> getStandingsBySeason(int season) {
        List<StandingResponse> standings = nbaStatsClient.fetchStandings(season);

        return standings.stream()
                .map(this::toStanding)
                .sorted(Comparator.comparingInt((Standing s) -> s.getConference().equals("East") ? 0 : 1)
                        .thenComparingInt(Standing::getWins).reversed()
                        .thenComparingInt(Standing::getLosses))
                .toList();
    }

    private Standing toStanding(StandingResponse source) {
        return Standing.builder()
                .teamId(source.getTeamId())
                .teamName(source.getTeamCity() + " " + source.getTeamName())
                .teamAbbr(source.getTeamSlug())
                .conference(source.getConference())
                .wins(source.getWins())
                .losses(source.getLosses())
                .winPercentage(source.getWinPercentage())
                .lastTenWins(parseLastTenWins(source.getLast10()))
                .lastTenLosses(parseLastTenLosses(source.getLast10()))
                .winStreak(source.getCurrentStreak())
                .build();
    }

    private int parseLastTenWins(String last10) {
        // Assuming last10 is like "5-5" or similar, parse wins
        if (last10 == null || !last10.contains("-")) return 0;
        String[] parts = last10.split("-");
        return Integer.parseInt(parts[0]);
    }

    private int parseLastTenLosses(String last10) {
        // Assuming last10 is like "5-5", parse losses
        if (last10 == null || !last10.contains("-")) return 0;
        String[] parts = last10.split("-");
        return Integer.parseInt(parts[1]);
    }
}
