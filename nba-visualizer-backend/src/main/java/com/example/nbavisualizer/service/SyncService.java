package com.example.nbavisualizer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final StandingsService standingsService;
    private final TeamService teamService;
    private final NbaRefreshProperties refreshProperties;

    @Scheduled(fixedDelayString = "${nba.refresh.standings-fixed-delay-ms:1200000}", initialDelayString = "${nba.refresh.initial-delay-ms:30000}")
    public void refreshCurrentStandings() {
        int season = currentSeason();
        withJitter("standings", () -> standingsService.refreshStandings(season));
    }

    @Scheduled(fixedDelayString = "${nba.refresh.metadata-fixed-delay-ms:86400000}", initialDelayString = "${nba.refresh.initial-delay-ms:30000}")
    public void refreshTeamMetadata() {
        withJitter("teams", teamService::refreshTeams);
    }

    private void withJitter(String label, Runnable refresh) {
        int delaySeconds = ThreadLocalRandom.current().nextInt(
                refreshProperties.jitterMinSeconds(),
                refreshProperties.jitterMaxSeconds() + 1
        );
        try {
            Thread.sleep(delaySeconds * 1000L);
            refresh.run();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("{} refresh interrupted during jitter", label);
        } catch (Exception ex) {
            log.warn("{} refresh failed: {}", label, ex.getMessage());
        }
    }

    private int currentSeason() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return today.getMonthValue() >= 10 ? today.getYear() + 1 : today.getYear();
    }
}
