package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.TeamDepthChart;
import com.example.nbavisualizer.model.id.TeamDepthChartId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamDepthChartRepository extends JpaRepository<TeamDepthChart, TeamDepthChartId> {
    List<TeamDepthChart> findByTeamIdAndSeasonOrderByPositionAscDepthOrderAsc(Integer teamId, Integer season);
    void deleteByTeamIdAndSeason(Integer teamId, Integer season);
}
