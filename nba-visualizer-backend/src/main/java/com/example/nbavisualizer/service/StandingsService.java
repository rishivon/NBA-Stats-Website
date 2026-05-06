package com.example.nbavisualizer.service;

import com.example.nbavisualizer.client.nbaStats.NbaStatsClient;
import com.example.nbavisualizer.client.nbaStats.StandingResponse;
import com.example.nbavisualizer.model.Standing;
import com.example.nbavisualizer.model.Team;
import com.example.nbavisualizer.repository.StandingRepository;
import com.example.nbavisualizer.repository.TeamRepository;
import com.example.nbavisualizer.service.cache.NbaCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class StandingsService {

    private final NbaStatsClient nbaStatsClient;
    private final StandingRepository standingRepository;
    private final TeamRepository teamRepository;
    private final NbaCacheService cacheService;
    private final NbaRefreshProperties refreshProperties;
    private final TaskExecutor taskExecutor;

    private final AtomicBoolean standingsRefreshInFlight = new AtomicBoolean(false);

    @Value("${nba.cache.standings-ttl-seconds:600}")
    private long standingsTtlSeconds;

    public List<Standing> getStandingsBySeason(int season) {
        String cacheKey = standingsCacheKey(season);
        return cacheService.get(cacheKey, new TypeReference<List<Standing>>() {})
                .orElseGet(() -> getStandingsFromDbOrRefresh(season, cacheKey));
    }

    @Transactional
    public List<Standing> refreshStandings(int season) {
        log.info("Refreshing standings for season {} from proxy", season);
        List<StandingResponse> standingsResponse = nbaStatsClient.fetchStandings(season);
        seedMissingTeams(standingsResponse);

        List<Standing> standings = standingsResponse.stream()
                .map(this::toStanding)
                .map(standing -> {
                    standing.setSeason(season);
                    standing.setLastUpdated(Instant.now());
                    return standing;
                })
                .toList();

        standingRepository.saveAll(standings);
        List<Standing> sorted = sortStandings(standings);
        cacheService.put(standingsCacheKey(season), sorted, Duration.ofSeconds(standingsTtlSeconds));
        return sorted;
    }

    private void seedMissingTeams(List<StandingResponse> standingsResponse) {
        List<Team> missingTeams = standingsResponse.stream()
                .filter(source -> source.getTeamId() != null && !teamRepository.existsById(source.getTeamId()))
                .map(source -> Team.builder()
                        .id(source.getTeamId())
                        .city(source.getTeamCity())
                        .name(source.getTeamName())
                        .fullName(source.getTeamCity() + " " + source.getTeamName())
                        .abbreviation(source.getTeamSlug())
                        .conference(source.getConference())
                        .division(source.getDivision())
                        .logoPath("/logos/" + safeLogoSlug(source) + ".svg")
                        .lastMetadataUpdate(Instant.now())
                        .build())
                .toList();

        if (!missingTeams.isEmpty()) {
            teamRepository.saveAll(missingTeams);
        }
    }

    private String safeLogoSlug(StandingResponse source) {
        if (source.getTeamSlug() != null) {
            return source.getTeamSlug().toLowerCase();
        }
        if (source.getTeamName() != null) {
            return source.getTeamName().toLowerCase().replace(" ", "-");
        }
        return String.valueOf(source.getTeamId());
    }

    private List<Standing> getStandingsFromDbOrRefresh(int season, String cacheKey) {
        List<Standing> dbStandings = standingRepository.findBySeason(season);
        if (!dbStandings.isEmpty()) {
            List<Standing> sorted = sortStandings(dbStandings);
            cacheService.put(cacheKey, sorted, Duration.ofSeconds(standingsTtlSeconds));
            if (isStale(dbStandings)) {
                refreshStandingsInBackground(season);
            }
            return sorted;
        }

        return refreshStandings(season);
    }

    public void refreshStandingsInBackground(int season) {
        if (!standingsRefreshInFlight.compareAndSet(false, true)) {
            return;
        }

        taskExecutor.execute(() -> {
            try {
                refreshStandings(season);
            } catch (Exception ex) {
                log.warn("Background standings refresh failed for season {}: {}", season, ex.getMessage());
            } finally {
                standingsRefreshInFlight.set(false);
            }
        });
    }

    private boolean isStale(List<Standing> standings) {
        Instant oldestUpdate = standings.stream()
                .map(Standing::getLastUpdated)
                .filter(java.util.Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(Instant.EPOCH);
        return oldestUpdate.isBefore(Instant.now().minus(Duration.ofMinutes(refreshProperties.standingsStaleMinutes())));
    }

    private List<Standing> sortStandings(List<Standing> standings) {
        return standings.stream()
                .sorted(Comparator.comparingInt((Standing s) -> "East".equals(s.getConference()) ? 0 : 1)
                        .thenComparing(Comparator.comparing(Standing::getConferenceRank, Comparator.nullsLast(Integer::compareTo)))
                        .thenComparing(Comparator.comparing(Standing::getWins, Comparator.nullsLast(Integer::compareTo)).reversed())
                        .thenComparing(Comparator.comparing(Standing::getLosses, Comparator.nullsLast(Integer::compareTo))))
                .toList();
    }

    private Standing toStanding(StandingResponse source) {
        return Standing.builder()
                .teamId(source.getTeamId())
                .teamName(source.getTeamCity() + " " + source.getTeamName())
                .teamAbbr(source.getTeamSlug())
                .conference(source.getConference())
                .division(source.getDivision())
                .wins(source.getWins())
                .losses(source.getLosses())
                .winPercentage(source.getWinPercentage())
                .conferenceRank(source.getConferenceRank())
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

    private String standingsCacheKey(int season) {
        return "standings:" + season;
    }
}
