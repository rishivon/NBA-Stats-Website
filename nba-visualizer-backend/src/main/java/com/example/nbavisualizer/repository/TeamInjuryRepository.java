package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.TeamInjury;
import com.example.nbavisualizer.model.id.TeamInjuryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamInjuryRepository extends JpaRepository<TeamInjury, TeamInjuryId> {
    List<TeamInjury> findByTeamIdOrderByPlayerNameAsc(Integer teamId);
    void deleteByTeamId(Integer teamId);
}
