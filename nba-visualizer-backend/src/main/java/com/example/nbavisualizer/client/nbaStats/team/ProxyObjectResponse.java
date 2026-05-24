package com.example.nbavisualizer.client.nbaStats.team;

import lombok.Data;

@Data
public class ProxyObjectResponse<T> {
    private T data;
    private String status;
    private String season;
}
