package com.example.nbavisualizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    private Integer id;
    private String abbreviation;
    private String city;
    private String conference;
    private String division;
    private String fullName;
    private String name;
}
