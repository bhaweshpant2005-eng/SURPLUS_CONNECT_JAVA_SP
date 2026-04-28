package com.example.surplusconnect.service;

import com.example.surplusconnect.model.NGO;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Geo-Optimized Matching & Routing
 * 
 * Feature 6: Use Graph + Dijkstra to find nearest NGO and optimal routes.
 */
@Service
public class GeoRoutingService {

    /**
     * Graph Node representing an NGO location
     */
    static class Node implements Comparable<Node> {
        final Long id;
        final double latitude;
        final double longitude;
        double minDistance = Double.MAX_VALUE;
        Node previous;
        final List<Edge> adjacencies = new ArrayList<>();

        Node(Long id, double lat, double lon) {
            this.id = id;
            this.latitude = lat;
            this.longitude = lon;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(minDistance, other.minDistance);
        }
    }

    /**
     * Graph Edge representing distance between nodes
     */
    static class Edge {
        final Node target;
        final double weight;

        Edge(Node target, double weight) {
            this.target = target;
            this.weight = weight;
        }
    }

    /**
     * Finds the nearest NGO from a starting coordinate using Dijkstra-like logic
     * in a spatial graph of NGOs.
     */
    public List<NGO> findOptimalPath(double startLat, double startLon, List<NGO> candidates) {
        if (candidates.isEmpty()) return candidates;

        Map<Long, Node> nodes = new HashMap<>();
        Node startNode = new Node(-1L, startLat, startLon);
        startNode.minDistance = 0;

        // Build Graph: Every node is connected to every other node with Haversine distance
        // (In a real scenario, we'd use road-network constraints)
        for (NGO ngo : candidates) {
            nodes.put(ngo.getId(), new Node(ngo.getId(), ngo.getLatitude(), ngo.getLongitude()));
        }

        // Connect start to all NGOs
        for (Node target : nodes.values()) {
            double dist = haversine(startLat, startLon, target.latitude, target.longitude);
            startNode.adjacencies.add(new Edge(target, dist));
        }

        // Connect NGOs to each other (Full mesh for this demo)
        for (Node n1 : nodes.values()) {
            for (Node n2 : nodes.values()) {
                if (n1 == n2) continue;
                double dist = haversine(n1.latitude, n1.longitude, n2.latitude, n2.longitude);
                n1.adjacencies.add(new Edge(n2, dist));
            }
        }

        // Run Dijkstra
        PriorityQueue<Node> queue = new PriorityQueue<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node u = queue.poll();

            for (Edge e : u.adjacencies) {
                Node v = e.target;
                double weight = e.weight;
                double distanceThroughU = u.minDistance + weight;
                if (distanceThroughU < v.minDistance) {
                    queue.remove(v);
                    v.minDistance = distanceThroughU;
                    v.previous = u;
                    queue.add(v);
                }
            }
        }

        // Sort candidates by the computed minDistance
        List<NGO> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble(n -> nodes.get(n.getId()).minDistance));

        return sorted;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
