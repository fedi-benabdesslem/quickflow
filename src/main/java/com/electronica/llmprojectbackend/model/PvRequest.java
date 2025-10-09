package com.electronica.llmprojectbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pv_requests")
public class PvRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateType templateType;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private String location;

    @ElementCollection
    @CollectionTable(name = "pv_participants", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "participant")
    private List<String> participants;

    @ElementCollection
    @CollectionTable(name = "pv_bullet_points", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "bullet_point")
    private List<String> bulletPoints;

    @Column(nullable = false)
    private Long userId;

    private LocalTime closingTime;

    @Column(columnDefinition = "TEXT")
    private String llmOutput;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


