package com.example.nbavisualizer.service;

import com.example.nbavisualizer.client.nbaStats.NbaStatsClient;
import com.example.nbavisualizer.model.Team;
import com.example.nbavisualizer.repository.TeamRepository;
import com.example.nbavisualizer.service.cache.NbaCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final NbaStatsClient nbaStatsClient;
    private final TeamRepository teamRepository;
    private final NbaCacheService cacheService;
    private final NbaRefreshProperties refreshProperties;
    private final TaskExecutor taskExecutor;

    private final AtomicBoolean teamsRefreshInFlight = new AtomicBoolean(false);

    @Value("${nba.cache.teams-ttl-seconds:2592000}")
    private long teamsTtlSeconds;

    public List<Team> getTeams() {
        return cacheService.get("teams:all", new TypeReference<List<Team>>() {})
                .orElseGet(this::getTeamsFromDbOrRefresh);
    }

    public Team getTeam(Integer id) {
        String cacheKey = "teams:" + id;
        return cacheService.get(cacheKey, new TypeReference<Team>() {})
                .orElseGet(() -> getTeamFromDbOrRefresh(id, cacheKey));
    }

    @Transactional
    public List<Team> refreshTeams() {
        log.info("Refreshing team metadata from proxy");
        List<Team> teams = nbaStatsClient.fetchTeams().stream()
                .map(this::normalizeTeam)
                .toList();

        teamRepository.saveAll(teams);
        List<Team> sorted = sortTeams(teams);
        cacheService.put("teams:all", sorted, Duration.ofSeconds(teamsTtlSeconds));
        sorted.forEach(team -> cacheService.put("teams:" + team.getId(), team, Duration.ofSeconds(teamsTtlSeconds)));
        return sorted;
    }

    private List<Team> getTeamsFromDbOrRefresh() {
        List<Team> teams = teamRepository.findAll();
        if (!teams.isEmpty()) {
            List<Team> sorted = sortTeams(teams);
            cacheService.put("teams:all", sorted, Duration.ofSeconds(teamsTtlSeconds));
            if (isStale(teams)) {
                refreshTeamsInBackground();
            }
            return sorted;
        }

        return refreshTeams();
    }

    private Team getTeamFromDbOrRefresh(Integer id, String cacheKey) {
        return teamRepository.findById(id)
                .map(team -> {
                    cacheService.put(cacheKey, team, Duration.ofSeconds(teamsTtlSeconds));
                    if (isStale(List.of(team))) {
                        refreshTeamsInBackground();
                    }
                    return team;
                })
                .orElseGet(() -> refreshTeams().stream()
                        .filter(team -> id.equals(team.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found: " + id)));
    }

    private void refreshTeamsInBackground() {
        if (!teamsRefreshInFlight.compareAndSet(false, true)) {
            return;
        }

        taskExecutor.execute(() -> {
            try {
                refreshTeams();
            } catch (Exception ex) {
                log.warn("Background team metadata refresh failed: {}", ex.getMessage());
            } finally {
                teamsRefreshInFlight.set(false);
            }
        });
    }

    private boolean isStale(List<Team> teams) {
        Instant oldestUpdate = teams.stream()
                .map(Team::getLastMetadataUpdate)
                .filter(java.util.Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(Instant.EPOCH);
        return oldestUpdate.isBefore(Instant.now().minus(Duration.ofDays(refreshProperties.metadataStaleDays())));
    }

    private Team normalizeTeam(Team team) {
        String logoSlug = logoSlug(team);
        team.setLogoPath("/logos/" + logoSlug + ".svg");
        team.setLastMetadataUpdate(Instant.now());
        return team;
    }

    private String logoSlug(Team team) {
        String name = team.getName() != null ? team.getName() : team.getFullName();
        if (name == null) {
            return team.getAbbreviation().toLowerCase();
        }
        return switch (name) {
            case "Trail Blazers" -> "blazers";
            case "76ers" -> "sixers";
            default -> name.toLowerCase().replace(" ", "-");
        };
    }

    private List<Team> sortTeams(List<Team> teams) {
        return teams.stream()
                .sorted(Comparator.comparing(Team::getFullName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}
