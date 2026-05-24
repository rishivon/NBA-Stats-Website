package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.TeamRosterPlayer;
import com.example.nbavisualizer.model.id.TeamPlayerSeasonId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRosterPlayerRepository extends JpaRepository<TeamRosterPlayer, TeamPlayerSeasonId> {
    List<TeamRosterPlayer> findByTeamIdAndSeasonOrderByPositionAscFullNameAsc(Integer teamId, Integer season);
}
