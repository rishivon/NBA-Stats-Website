package com.example.nbavisualizer.service;

import com.example.nbavisualizer.client.nbaStats.NbaStatsClient;
import com.example.nbavisualizer.model.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final NbaStatsClient nbaStatsClient;

    public List<Team> getTeams() {
        return nbaStatsClient.fetchTeams().stream()
                .sorted(Comparator.comparing(Team::getFullName))
                .toList();
    }
}
