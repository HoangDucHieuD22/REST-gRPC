package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class restpath_dijkstra {

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
        String qCode = "pTChB07F";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/path?studentCode=" + studentCode + "&qCode=" + qCode)).build();
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

        for (JsonNode edgeNode : edgesNode) {
            JsonNode fromNode = edgeNode.get("from");
            JsonNode toNode = edgeNode.get("to");
            JsonNode weightNode = edgeNode.get("weight");

            if (fromNode != null && toNode != null && weightNode != null) {
                String from = fromNode.asText();
                String to = toNode.asText();
                int weight = weightNode.asInt();
                if (adj.containsKey(from)) {
                    adj.get(from).add(new Edge(to, weight));
                }
            }
        }

        // c. Tìm đường đi ngắn nhất bằng Dijkstra
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();

        nodesNode.forEach(node -> distances.put(node.asText(), Integer.MAX_VALUE));

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
            answer = "";
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

        // e. Gửi GET request để nộp bài (Server yêu cầu GET)
        String submitUrl = String.format("%s/api/rest/path/submit?studentCode=%s&qCode=%s&requestId=%s&answer=%s",
                base,
                URLEncoder.encode(studentCode, StandardCharsets.UTF_8),
                URLEncoder.encode(qCode, StandardCharsets.UTF_8),
                URLEncoder.encode(requestId, StandardCharsets.UTF_8),
                URLEncoder.encode(answer, StandardCharsets.UTF_8)
        );

        var submitReq = HttpRequest.newBuilder(URI.create(submitUrl))
                .GET()
                .build();

        var postResponse = client.send(submitReq, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response from server: " + postResponse.body());
    }
}