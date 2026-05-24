package com.example.nbavisualizer.controller;

import com.example.nbavisualizer.model.Team;
import com.example.nbavisualizer.model.dashboard.TeamDashboard;
import com.example.nbavisualizer.model.dashboard.TeamScheduleGame;
import com.example.nbavisualizer.service.TeamDashboardService;
import com.example.nbavisualizer.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamDashboardService teamDashboardService;

    @GetMapping
    public List<Team> getTeams() {
        return teamService.getTeams();
    }

    @GetMapping("/{id}")
    public Team getTeam(@PathVariable Integer id) {
        return teamService.getTeam(id);
    }

    @GetMapping("/{id}/dashboard")
    public TeamDashboard getTeamDashboard(@PathVariable Integer id, @RequestParam(required = false) Integer season) {
        return teamDashboardService.getTeamDashboard(id, season);
    }

    @GetMapping("/{id}/schedule")
    public List<TeamScheduleGame> getTeamSchedule(@PathVariable Integer id, @RequestParam(required = false) Integer season) {
        return teamDashboardService.getTeamSchedule(id, season);
    }

    @GetMapping("/{id}/roster")
    public TeamDashboard getTeamRoster(@PathVariable Integer id, @RequestParam(required = false) Integer season) {
        return teamDashboardService.getTeamRosterDashboard(id, season);
    }
}
