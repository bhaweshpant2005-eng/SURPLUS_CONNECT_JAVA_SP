# 🌱 Surplus Connect — Smart Resource Matching Platform

> A Java Spring Boot based platform that bridges surplus resources (food, clothes, essentials) with NGOs and communities in need, using advanced DSA, OOP, and system design principles.

---

## 📌 Table of Contents
- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Data Structures & Algorithms](#data-structures--algorithms)
- [System Architecture](#system-architecture)
- [Setup & Running](#setup--running)
- [API Endpoints](#api-endpoints)
- [Roles & Access](#roles--access)
- [Screenshots](#screenshots)

---

## Overview

Surplus Connect is a full-stack web application that:
- Allows **Donors** to register surplus items (food, clothes, essentials)
- Allows **NGOs** to submit resource requests
- Automatically **matches donations with requests** using a greedy allocation algorithm
- Tracks the full **lifecycle** of each donation from registration to delivery
- Provides **analytics, reviews, notifications**, and an **admin dashboard**

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2 |
| Security | Spring Security + JWT |
| Database | MySQL 8 + Spring Data JPA / Hibernate |
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| Build Tool | Maven |
| Background | Particles.js |
| Icons | Font Awesome 6 |

---

## Features

### Core Features
| # | Feature | Description |
|---|---------|-------------|
| 1 | Smart Resource Splitting | Greedy algorithm splits one donation across multiple NGOs |
| 2 | Constraint-Based Matching | Filter pipeline: compatibility → accepted types → distance → capacity |
| 3 | Dynamic Re-Prioritization | Aging algorithm recalculates request priority over time |
| 4 | Multi-Hop Allocation | BFS graph traversal for indirect NGO transfers |
| 5 | Capacity-Aware Allocation | Prevents NGO overloading via load balancing |
| 6 | Conflict Resolution | Weighted scoring: proximity + priority + fairness + trust |
| 7 | Resource Compatibility Matrix | O(1) lookup for NGO-resource type compatibility |
| 8 | Time-Slot Scheduling | Greedy interval scheduling for delivery optimization |
| 9 | Backup Matching Pool | Failover to secondary NGOs when primary allocation fails |
| 10 | Multi-Level Queue System | 4-level priority queue: CRITICAL → HIGH → NORMAL → LOW |
| 11 | Recommendation Engine | Rule-based deficit analysis for donation suggestions |
| 12 | Geographic Clustering | K-Means clustering of NGOs by location |
| 13 | Transaction Rollback | Saga pattern for compensating failed allocations |
| 14 | Duplicate Detection | SHA-256 hashing for donation/request deduplication |
| 15 | Weighted Fair Distribution | Fairness-aware allocation preventing monopolization |
| 16 | Resource Lifecycle FSM | Finite State Machine: PENDING → MATCHED → CONFIRMED → DISPATCHED → DELIVERED |
| 17 | Simulation & Stress Test | Monte Carlo data generation + algorithm benchmarking |
| 18 | Emergency Mode | Override scoring for CRITICAL urgency requests |
| 19 | Dependency-Based Allocation | Bundle related items (Rice + Dal) for same NGO |
| 20 | Adaptive Thresholds | Dynamic minimum quantity based on supply/demand ratio |
| 21 | Review & Rating System | EMA-based rating with SHA-256 duplicate prevention |
| 22 | Image Upload | Donation and review image upload with type validation |

---

## Data Structures & Algorithms

| DSA Concept | Where Used |
|-------------|-----------|
| **HashMap** | Duplicate detection (content hash → item), compatibility matrix |
| **Priority Queue** | Multi-level request queue (CRITICAL → LOW) |
| **Queue (LinkedList)** | Notification queue, backup NGO pool |
| **Graph (Adjacency List)** | NGO transfer network for multi-hop allocation |
| **BFS** | Shortest transfer path between NGOs |
| **Dijkstra** | Geo-optimized nearest NGO routing |
| **K-Means Clustering** | Geographic NGO grouping |
| **Greedy Algorithm** | Resource splitting, interval scheduling |
| **FSM** | Resource lifecycle state transitions |
| **SHA-256 Hashing** | Duplicate detection for donations, requests, reviews |
| **TreeMap (BST)** | NGO leaderboard sorted by performance score |
| **EMA (Exponential Moving Average)** | NGO rating updates |
| **Saga Pattern** | Transaction rollback for failed allocations |

---

## System Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Frontend (HTML/CSS/JS)              │
│  Donor UI │ NGO UI │ Admin Dashboard │ Reviews        │
└─────────────────────┬───────────────────────────────┘
                      │ REST API (JWT Auth)
┌─────────────────────▼───────────────────────────────┐
│              Spring Boot Backend                      │
│                                                       │
│  Controllers → Services → Repositories               │
│                                                       │
│  MatchingService    LifecycleManager                 │
│  GraphAllocation    SchedulingService                │
│  TrustService       FraudDetection                   │
│  AnalyticsService   NotificationService              │
└─────────────────────┬───────────────────────────────┘
                      │ JPA / Hibernate
┌─────────────────────▼───────────────────────────────┐
│                  MySQL Database                       │
│  users │ item │ ngo │ resource_request               │
│  allocation │ review │ notification │ time_slot      │
└─────────────────────────────────────────────────────┘
```

---

## Setup & Running

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- XAMPP (or any MySQL server)

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/bhaweshpant2005-eng/SURPLUS_CONNECT_JAVA_SP.git
cd SURPLUS_CONNECT_JAVA_SP
```

**2. Create the database**
```sql
CREATE DATABASE surplus_db;
```

**3. Configure database credentials**

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/surplus_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

**4. Run the application**
```bash
mvn spring-boot:run
```

**5. Open in browser**
```
http://localhost:8080
```

---

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Donations (Items)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/items` | Get all donations |
| POST | `/api/items` | Register new donation (DONOR only) |
| POST | `/api/items/{id}/upload-image` | Upload item image |
| DELETE | `/api/items/{id}` | Delete donation |

### Requests
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/requests` | Get all requests |
| POST | `/api/requests` | Submit resource request (NGO only) |
| GET | `/api/requests/ngo/{ngoId}` | Get requests by NGO |

### Matching
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/matching/split/{itemId}` | Trigger matching for an item |
| POST | `/api/matching/reprioritize` | Recalculate all priorities |
| GET | `/api/matching/queues` | View multi-level queues |
| POST | `/api/matching/emergency/activate` | Activate emergency mode |
| GET | `/api/matching/recommendations` | Get donation recommendations |

### Reviews
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/reviews` | Submit a review |
| GET | `/api/reviews/target/{id}?targetType=NGO` | Get reviews for target |
| GET | `/api/reviews/stats/{id}?targetType=NGO` | Get rating statistics |

### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/analytics` | System analytics |
| GET | `/api/admin/leaderboard` | NGO leaderboard |

---

## Roles & Access

| Role | Capabilities |
|------|-------------|
| **DONOR** | Register donations, upload images, view impact, leave reviews |
| **NGO** | Submit requests, view available donations, leave reviews |
| **ADMIN** | Full access: analytics, leaderboard, matching control, all data |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/example/surplusconnect/
│   │   ├── config/          # CORS, Exception Handler
│   │   ├── controller/      # REST Controllers (9 controllers)
│   │   ├── model/           # JPA Entities (11 models)
│   │   ├── repository/      # Spring Data Repositories (8 repos)
│   │   ├── security/        # JWT Filter, Security Config
│   │   └── service/         # Business Logic (16 services)
│   └── resources/
│       ├── application.properties
│       └── static/          # Frontend (HTML, CSS, JS)
```

---

## Academic Highlights

This project demonstrates:
- **22+ advanced features** integrating DSA concepts
- **Clean OOP design** with proper layering (Controller → Service → Repository)
- **Design Patterns**: Observer, Saga, Command, Strategy, FSM
- **Security**: JWT authentication, RBAC, input validation, file type checking
- **Real-world algorithms**: K-Means, BFS, Dijkstra, Greedy, EMA
- **Event-driven architecture**: automatic matching on donation/request creation

---

## Author

**Bhawesh Pant**  
Java Spring Boot | DSA | System Design  
GitHub: [@bhaweshpant2005-eng](https://github.com/bhaweshpant2005-eng)
