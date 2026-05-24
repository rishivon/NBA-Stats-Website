package com.example.nbavisualizer.model;

import com.example.nbavisualizer.model.id.TeamInjuryId;
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
@Table(name = "team_injuries")
@IdClass(TeamInjuryId.class)
public class TeamInjury {
    @Id
    private Integer teamId;
    @Id
    private String playerName;
    private String position;
    private String injury;
    private String expectedReturn;
    private String status;
    private Instant lastUpdated;
}
