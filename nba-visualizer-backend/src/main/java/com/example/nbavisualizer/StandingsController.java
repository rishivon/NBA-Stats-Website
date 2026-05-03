package com.example.nbavisualizer;

import com.example.nbavisualizer.model.Standing;
import com.example.nbavisualizer.service.StandingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
public class StandingsController {

    private final StandingsService standingsService;

    @GetMapping
    public List<Standing> getStandings(@RequestParam(defaultValue = "2026") int season) {
        return standingsService.getStandingsBySeason(season);
    }
}
