package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class restobject_dijkstra {

    // Helper class for edges in the graph
    static class Edge {
        String target;
        int cost;

        Edge(String target, int cost) {
            this.target = target;
            this.cost = cost;
        }
    }

    // Helper class for the priority queue in Dijkstra's algorithm
    static class Node implements Comparable<Node> {
        String name;
        int cost;

        Node(String name, int cost) {
            this.name = name;
            this.cost = cost;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.cost, other.cost);
        }
    }

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "pRlmexZl";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/object?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        JsonNode dataNode = responseNode.get("data");
        String startNodeName = dataNode.get("start").asText();
        String endNodeName = dataNode.get("end").asText();
        ArrayNode edgesNode = (ArrayNode) dataNode.get("edges");
        ArrayNode nodesNode = (ArrayNode) dataNode.get("nodes");

        // Build graph representation
        Map<String, List<Edge>> adj = new HashMap<>();
        nodesNode.forEach(node -> adj.put(node.asText(), new ArrayList<>()));
        
        // Corrected field names: from, to, weight
        for (JsonNode edgeNode : edgesNode) {
            JsonNode sourceNode = edgeNode.get("from");
            JsonNode targetNode = edgeNode.get("to");
            JsonNode costNode = edgeNode.get("weight");

            if (sourceNode != null && targetNode != null && costNode != null) {
                String source = sourceNode.asText();
                String target = targetNode.asText();
                int cost = costNode.asInt();
                if (adj.containsKey(source)) {
                    adj.get(source).add(new Edge(target, cost));
                }
            }
        }

        // c. Tìm đường đi ngắn nhất bằng Dijkstra
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();

        // Initialize distances for all nodes
        for (JsonNode node : nodesNode) {
            distances.put(node.asText(), Integer.MAX_VALUE);
        }

        // Set distance for start node
        if (distances.containsKey(startNodeName)) {
            distances.put(startNodeName, 0);
            pq.add(new Node(startNodeName, 0));
        }

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();
            String u = currentNode.name;

            if (u.equals(endNodeName)) {
                break; 
            }

            if (currentNode.cost > distances.get(u)) {
                continue;
            }

            if (adj.containsKey(u)) {
                for (Edge edge : adj.get(u)) {
                    String v = edge.target;
                    if (distances.containsKey(v)) {
                        int newDist = distances.get(u) + edge.cost;
                        if (newDist < distances.get(v)) {
                            distances.put(v, newDist);
                            predecessors.put(v, u);
                            pq.add(new Node(v, newDist));
                        }
                    }
                }
            }
        }

        // d. Reconstruct path and format answer
        String answer;
        Integer finalCost = distances.get(endNodeName);
        if (finalCost == null || finalCost == Integer.MAX_VALUE) {
            answer = ""; // No path found
        } else {
            List<String> path = new ArrayList<>();
            String current = endNodeName;
            while (current != null) {
                path.add(current);
                current = predecessors.get(current);
            }
            Collections.reverse(path);
            String pathStr = String.join("->", path);
            answer = finalCost + "|" + pathStr;
        }

        // e. Gửi POST request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/object/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        var postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response from server: " + postResponse.body());
    }
}