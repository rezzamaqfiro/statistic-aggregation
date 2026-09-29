package com.example.stataggregation.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;

@Entity
@Table (name = "PLAYER_STATS")
@Data  @NoArgsConstructor @AllArgsConstructor 
public class PlayerStatEntity {
    @Id
    @Column(name = "PLAYER_ID")
    private String playerId;

    @Column(name = "KILLS")
    private long kills;

    @Column(name = "WINS")
    private long wins;

    @Column(name = "PLAYTIME_SECONDS")
    private long playtimeSeconds;
}