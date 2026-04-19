package com.queueless.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Organisation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String type; // e.g., Hospital, Bank
    private String prefix; // e.g., H, B
}