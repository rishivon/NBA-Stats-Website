package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.TeamPlayerStats;
import com.example.nbavisualizer.model.id.TeamPlayerSeasonId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamPlayerStatsRepository extends JpaRepository<TeamPlayerStats, TeamPlayerSeasonId> {
    List<TeamPlayerStats> findByTeamIdAndSeason(Integer teamId, Integer season);
}
