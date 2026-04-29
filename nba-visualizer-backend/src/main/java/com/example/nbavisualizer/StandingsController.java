package com.example.nbavisualizer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/standings")
public class StandingsController {
    @GetMapping("/teams")
    public List<String> getTeams() {
        return Arrays.asList(
            "Boston Celtics",
            "Golden State Warriors",
            "Los Angeles Lakers",
            "Milwaukee Bucks",
            "Miami Heat",
            "Brooklyn Nets",
            "Phoenix Suns",
            "Philadelphia 76ers"
        );
    }
}
