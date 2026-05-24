package com.example.nbavisualizer.client.nbaStats;

import com.example.nbavisualizer.model.Team;
import com.example.nbavisualizer.client.nbaStats.team.ProxyListResponse;
import com.example.nbavisualizer.client.nbaStats.team.ProxyObjectResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamGameLogResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamDepthChartResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamInjuryResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamPlayerStatsResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamRosterPlayerResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamSeasonStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NbaStatsClient {

    private final RestTemplate restTemplate;

    @Value("${nba.proxy.base-url:http://localhost:8000}")
    private String proxyBaseUrl;

    public List<Team> fetchTeams() {
        String proxyUrl = proxyBaseUrl + "/teams";
        log.info("Fetching teams from NBA Stats Proxy: {}", proxyUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<List<Team>> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<Team>>() {}
        );

        if (response.getBody() == null) {
            log.warn("Proxy teams response is empty");
            return List.of();
        }

        log.info("Successfully fetched {} teams from NBA Stats Proxy", response.getBody().size());
        return response.getBody();
    }

    public List<StandingResponse> fetchStandings(int season) {
        String proxyUrl = proxyBaseUrl + "/standings?season=" + season;
        log.info("Fetching standings from NBA Stats Proxy: {}", proxyUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<StandingsResponseWrapper> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request,
                StandingsResponseWrapper.class
        );

        if (response.getBody() == null || response.getBody().getData() == null) {
            log.warn("Proxy standings response is empty");
            return List.of();
        }

        log.info("Successfully fetched {} standings from NBA Stats Proxy", response.getBody().getCount());
        return response.getBody().getData();
    }

    public List<TeamRosterPlayerResponse> fetchTeamRoster(Integer teamId, Integer season) {
        String proxyUrl = proxyBaseUrl + "/team-roster/" + teamId + "?season=" + season;
        ResponseEntity<ProxyListResponse<TeamRosterPlayerResponse>> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request(),
                new ParameterizedTypeReference<ProxyListResponse<TeamRosterPlayerResponse>>() {}
        );
        return response.getBody() == null || response.getBody().getData() == null ? List.of() : response.getBody().getData();
    }

    public TeamSeasonStatsResponse fetchTeamStats(Integer teamId, Integer season) {
        String proxyUrl = proxyBaseUrl + "/team-stats/" + teamId + "?season=" + season;
        ResponseEntity<ProxyObjectResponse<TeamSeasonStatsResponse>> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request(),
                new ParameterizedTypeReference<ProxyObjectResponse<TeamSeasonStatsResponse>>() {}
        );
        return response.getBody() == null ? null : response.getBody().getData();
    }

    public List<TeamSeasonStatsResponse> fetchLeagueTeamStats(Integer season) {
        String proxyUrl = proxyBaseUrl + "/league-team-stats?season=" + season;
        ResponseEntity<ProxyListResponse<TeamSeasonStatsResponse>> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request(),
                new ParameterizedTypeReference<ProxyListResponse<TeamSeasonStatsResponse>>() {}
        );
        return response.getBody() == null || response.getBody().getData() == null ? List.of() : response.getBody().getData();
    }

    public List<TeamPlayerStatsResponse> fetchTeamPlayerStats(Integer teamId, Integer season) {
        String proxyUrl = proxyBaseUrl + "/team-player-stats/" + teamId + "?season=" + season;
        ResponseEntity<ProxyListResponse<TeamPlayerStatsResponse>> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request(),
                new ParameterizedTypeReference<ProxyListResponse<TeamPlayerStatsResponse>>() {}
        );
        return response.getBody() == null || response.getBody().getData() == null ? List.of() : response.getBody().getData();
    }

    public List<TeamGameLogResponse> fetchTeamGameLog(Integer teamId, Integer season) {
        String proxyUrl = proxyBaseUrl + "/team-game-log/" + teamId + "?season=" + season;
        ResponseEntity<ProxyListResponse<TeamGameLogResponse>> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request(),
                new ParameterizedTypeReference<ProxyListResponse<TeamGameLogResponse>>() {}
        );
        return response.getBody() == null || response.getBody().getData() == null ? List.of() : response.getBody().getData();
    }

    public List<TeamInjuryResponse> fetchTeamInjuries(Integer teamId) {
        String proxyUrl = proxyBaseUrl + "/team-injuries/" + teamId;
        ResponseEntity<ProxyListResponse<TeamInjuryResponse>> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request(),
                new ParameterizedTypeReference<ProxyListResponse<TeamInjuryResponse>>() {}
        );
        return response.getBody() == null || response.getBody().getData() == null ? List.of() : response.getBody().getData();
    }

    public List<TeamDepthChartResponse> fetchTeamDepthChart(Integer teamId) {
        String proxyUrl = proxyBaseUrl + "/team-depth-chart/" + teamId;
        ResponseEntity<ProxyListResponse<TeamDepthChartResponse>> response = restTemplate.exchange(
                proxyUrl,
                HttpMethod.GET,
                request(),
                new ParameterizedTypeReference<ProxyListResponse<TeamDepthChartResponse>>() {}
        );
        return response.getBody() == null || response.getBody().getData() == null ? List.of() : response.getBody().getData();
    }

    private HttpEntity<Void> request() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }
}
