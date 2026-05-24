package com.example.nbavisualizer.client.nbaStats.team;

import lombok.Data;

import java.util.List;

@Data
public class ProxyListResponse<T> {
    private List<T> data;
    private Integer count;
    private String status;
    private String season;
}
