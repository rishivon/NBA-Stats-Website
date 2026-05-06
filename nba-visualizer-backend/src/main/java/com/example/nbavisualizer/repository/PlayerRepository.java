package com.example.nbavisualizer.repository;

import com.example.nbavisualizer.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Integer> {
}
