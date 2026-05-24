package com.example.nbavisualizer.model;

import com.example.nbavisualizer.model.id.TeamPlayerSeasonId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "team_roster_players")
@IdClass(TeamPlayerSeasonId.class)
public class TeamRosterPlayer {
    @Id
    private Integer teamId;
    @Id
    private Integer season;
    @Id
    private Integer playerId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String position;
    private String jersey;
    private String height;
    private String weight;
    private String salary;
    private Instant lastUpdated;
}
