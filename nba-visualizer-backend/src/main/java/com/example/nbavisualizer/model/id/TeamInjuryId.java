package com.example.nbavisualizer.model.id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamInjuryId implements Serializable {
    private Integer teamId;
    private String playerName;
}
