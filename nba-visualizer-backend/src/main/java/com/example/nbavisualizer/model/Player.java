package com.example.nbavisualizer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "players")
public class Player {
    @Id
    private Integer id;
    private Integer teamId;
    private String firstName;
    private String lastName;
    private String position;
    private String height;
    private String weight;
    private Instant lastMetadataUpdate;
}
