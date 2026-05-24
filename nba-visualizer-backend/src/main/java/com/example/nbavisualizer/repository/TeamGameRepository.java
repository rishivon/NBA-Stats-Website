package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.TeamGame;
import com.example.nbavisualizer.model.id.TeamGameId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamGameRepository extends JpaRepository<TeamGame, TeamGameId> {
    List<TeamGame> findByTeamIdAndSeasonOrderByGameDateDesc(Integer teamId, Integer season);
}
