package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.Standing;
import com.example.nbavisualizer.model.id.StandingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<Standing, StandingId> {
    List<Standing> findBySeason(Integer season);
    Optional<Standing> findFirstByTeamIdOrderBySeasonDesc(Integer teamId);
}
