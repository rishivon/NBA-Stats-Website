package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.Standing;
import com.example.nbavisualizer.model.id.StandingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandingRepository extends JpaRepository<Standing, StandingId> {
    List<Standing> findBySeason(Integer season);
}
