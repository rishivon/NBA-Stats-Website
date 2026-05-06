package com.example.nbavisualizer.controller;

import com.example.nbavisualizer.model.Team;
import com.example.nbavisualizer.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public List<Team> getTeams() {
        return teamService.getTeams();
    }

    @GetMapping("/{id}")
    public Team getTeam(@PathVariable Integer id) {
        return teamService.getTeam(id);
    }
}
