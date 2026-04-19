package com.queueless.model;
import com.queueless.model.enums.TokenStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenNumber;

    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    @ManyToOne
    private User user;

    @ManyToOne
    private Organisation organisation;

    private LocalDateTime createdAt;
}