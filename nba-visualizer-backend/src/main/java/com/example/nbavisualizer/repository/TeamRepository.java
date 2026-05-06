package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Integer> {
}
