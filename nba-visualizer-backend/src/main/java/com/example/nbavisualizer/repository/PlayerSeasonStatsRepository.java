package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.PlayerSeasonStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerSeasonStatsRepository extends JpaRepository<PlayerSeasonStats, Integer> {
}
