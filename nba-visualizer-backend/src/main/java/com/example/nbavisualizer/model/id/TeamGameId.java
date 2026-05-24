package com.example.nbavisualizer.model.id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamGameId implements Serializable {
    private Integer teamId;
    private Integer season;
    private String gameId;
}
