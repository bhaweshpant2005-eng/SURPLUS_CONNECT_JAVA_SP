package com.example.surplusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDateTime createdAt;

    // --- NEW: Trust & Credibility System ---
    private Double trustScore = 80.0;
    private Integer successfulDonations = 0;
    private Integer cancelledDonations = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Default constructor
    public User() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getTrustScore() { return trustScore != null ? trustScore : 80.0; }
    public void setTrustScore(Double trustScore) { this.trustScore = trustScore; }

    public Integer getSuccessfulDonations() { return successfulDonations != null ? successfulDonations : 0; }
    public void setSuccessfulDonations(Integer successfulDonations) { this.successfulDonations = successfulDonations; }

    public Integer getCancelledDonations() { return cancelledDonations != null ? cancelledDonations : 0; }
    public void setCancelledDonations(Integer cancelledDonations) { this.cancelledDonations = cancelledDonations; }
}
