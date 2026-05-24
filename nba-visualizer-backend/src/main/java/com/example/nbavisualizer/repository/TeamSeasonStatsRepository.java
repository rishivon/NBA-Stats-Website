package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.TeamSeasonStats;
import com.example.nbavisualizer.model.id.TeamSeasonId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamSeasonStatsRepository extends JpaRepository<TeamSeasonStats, TeamSeasonId> {
    Optional<TeamSeasonStats> findByTeamIdAndSeason(Integer teamId, Integer season);
    List<TeamSeasonStats> findBySeason(Integer season);
}
