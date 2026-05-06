package com.example.nbavisualizer.client.nbaStats;

import com.example.nbavisualizer.model.Team;
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
}
