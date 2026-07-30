package com.javaproject.beans;

import java.util.List;
import lombok.Data;

@Data
public class BoardGame {
    private Long id;
    private String name;
    private int level;
    private int minPlayers;
    private String maxPlayers;  // must be String — DB is VARCHAR(50), seed data has '+'
    private String gameType;
    private List<Review> reviews;
}
