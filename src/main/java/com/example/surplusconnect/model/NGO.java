package com.example.surplusconnect.model;

import jakarta.persistence.*;

/**
 * NGO entity supporting:
 * - Capacity-Aware Allocation (Feature 5): maxCapacity / currentLoad
 * - Geographic Clustering (Feature 12): latitude / longitude
 * - Review & Rating (Feature 21): rating score
 * - Constraint-Based Matching (Feature 2): acceptedTypes, maxDistance
 * - Weighted Fair Distribution (Feature 15): totalReceived for proportional tracking
 */
@Entity
public class NGO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String contactPerson;
    private String phone;
    private String location;

    // --- Feature 12: Geographic Clustering ---
    private Double latitude;
    private Double longitude;

    // --- Feature 5: Capacity-Aware Allocation ---
    private Integer maxCapacity;
    private Integer currentLoad;

    // --- Feature 2: Constraint-Based Matching ---
    private String acceptedTypes;    // Comma-separated: "Food,Clothes"
    private Double maxDistance;       // Maximum pickup distance in km

    // --- Feature 21: Review & Rating ---
    private Double rating = 5.0;
    private Integer totalReviews;

    // --- Feature 15: Weighted Fair Distribution ---
    private Integer totalReceived;

    // --- Feature 7: Resource Compatibility ---
    private String ngoCategory;      // e.g., "Children's Home", "Shelter", "Hospital"

    // --- Feature 12: Cluster assignment ---
    private Integer clusterGroup = -1;

    // --- NEW: Trust & Credibility System ---
    private Double trustScore = 75.0; // Starting baseline
    private Integer allocationCount;
    private Integer rejectionCount;

    // --- NEW: Impact Score Engine ---
    private Double impactScore;
    private Integer peopleHelped;
    private Double wasteReducedKg;

    public NGO() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getLatitude() { return latitude != null ? latitude : 0.0; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude != null ? longitude : 0.0; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getMaxCapacity() { return maxCapacity != null ? maxCapacity : 0; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public Integer getCurrentLoad() { return currentLoad != null ? currentLoad : 0; }
    public void setCurrentLoad(Integer currentLoad) { this.currentLoad = currentLoad; }

    public String getAcceptedTypes() { return acceptedTypes; }
    public void setAcceptedTypes(String acceptedTypes) { this.acceptedTypes = acceptedTypes; }

    public Double getMaxDistance() { return maxDistance != null ? maxDistance : 0.0; }
    public void setMaxDistance(Double maxDistance) { this.maxDistance = maxDistance; }

    public Double getRating() { return rating != null ? rating : 5.0; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getTotalReviews() { return totalReviews != null ? totalReviews : 0; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Integer getTotalReceived() { return totalReceived != null ? totalReceived : 0; }
    public void setTotalReceived(Integer totalReceived) { this.totalReceived = totalReceived; }

    public String getNgoCategory() { return ngoCategory; }
    public void setNgoCategory(String ngoCategory) { this.ngoCategory = ngoCategory; }

    public Integer getClusterGroup() { return clusterGroup != null ? clusterGroup : -1; }
    public void setClusterGroup(Integer clusterGroup) { this.clusterGroup = clusterGroup; }

    // --- Trust System Getters/Setters ---
    public Double getTrustScore() { return trustScore != null ? trustScore : 75.0; }
    public void setTrustScore(Double trustScore) { this.trustScore = trustScore; }

    public Integer getAllocationCount() { return allocationCount != null ? allocationCount : 0; }
    public void setAllocationCount(Integer allocationCount) { this.allocationCount = allocationCount; }

    public Integer getRejectionCount() { return rejectionCount != null ? rejectionCount : 0; }
    public void setRejectionCount(Integer rejectionCount) { this.rejectionCount = rejectionCount; }

    // --- Impact System Getters/Setters ---
    public Double getImpactScore() { return impactScore != null ? impactScore : 0.0; }
    public void setImpactScore(Double impactScore) { this.impactScore = impactScore; }

    public Integer getPeopleHelped() { return peopleHelped != null ? peopleHelped : 0; }
    public void setPeopleHelped(Integer peopleHelped) { this.peopleHelped = peopleHelped; }

    public Double getWasteReducedKg() { return wasteReducedKg != null ? wasteReducedKg : 0.0; }
    public void setWasteReducedKg(Double wasteReducedKg) { this.wasteReducedKg = wasteReducedKg; }

    /**
     * Feature 5: Returns remaining capacity for allocation checks.
     */
    public int getAvailableCapacity() {
        return getMaxCapacity() - getCurrentLoad();
    }
}
