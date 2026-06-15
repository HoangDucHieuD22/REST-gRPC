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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class restpath_bitmaskDP {

    static final int INF = Integer.MAX_VALUE / 2; // Use a smaller infinity to prevent overflow

    // Helper class for Dijkstra
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
        String studentCode = "B22DCVT192";
        String qCode = "XDJE7dg3";
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/path?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu
        JsonNode data = responseNode.get("data");
        String startNode = data.get("start").asText();
        String endNode = data.get("end").asText();
        List<String> mandatory = StreamSupport.stream(data.get("mandatory").spliterator(), false)
                .map(JsonNode::asText).collect(Collectors.toList());

        // Build full graph
        Map<String, List<Node>> adj = new HashMap<>();
        data.get("nodes").forEach(node -> adj.put(node.asText(), new ArrayList<>()));
        data.get("edges").forEach(edge -> {
            String from = edge.get("from").asText();
            String to = edge.get("to").asText();
            int weight = edge.get("weight").asInt();
            if (adj.containsKey(from)) {
                adj.get(from).add(new Node(to, weight));
            }
        });

        // c. Tìm lộ trình ngắn nhất
        // 1. Identify points of interest and map them to indices
        List<String> points = new ArrayList<>();
        points.add(startNode);
        mandatory.forEach(p -> { if (!points.contains(p)) points.add(p); });
        if (!points.contains(endNode)) points.add(endNode);

        Map<String, Integer> pointIndexMap = new HashMap<>();
        for (int i = 0; i < points.size(); i++) {
            pointIndexMap.put(points.get(i), i);
        }

        int n = points.size();
        int startIdx = pointIndexMap.get(startNode);
        int endIdx = pointIndexMap.get(endNode);

        // 2. Pre-compute all-pairs shortest paths between points of interest using Dijkstra
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            dist[i] = dijkstra(points.get(i), adj, points, pointIndexMap);
        }

        // 3. Bitmask DP
        int[][] dp = new int[1 << n][n];
        int[][] parent = new int[1 << n][n];
        for (int[] row : dp) Arrays.fill(row, INF);

        dp[1 << startIdx][startIdx] = 0;

        for (int mask = 1; mask < (1 << n); mask++) {
            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) != 0) { // If u is in the set
                    for (int v = 0; v < n; v++) {
                        if (u != v && (mask & (1 << v)) != 0) { // If v is also in the set
                            int prevMask = mask ^ (1 << u);
                            if (dp[prevMask][v] != INF && dist[v][u] != INF) {
                                int newCost = dp[prevMask][v] + dist[v][u];
                                if (newCost < dp[mask][u]) {
                                    dp[mask][u] = newCost;
                                    parent[mask][u] = v;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Find final path and cost
        int finalMask = (1 << startIdx);
        for (String m : mandatory) {
            finalMask |= (1 << pointIndexMap.get(m));
        }
        finalMask |= (1 << endIdx);

        int finalCost = dp[finalMask][endIdx];
        String answer = "";

        if (finalCost != INF) {
            List<String> path = new ArrayList<>();
            int curr = endIdx;
            int mask = finalMask;
            while (mask != 0) {
                path.add(points.get(curr));
                int prev = parent[mask][curr];
                mask ^= (1 << curr);
                curr = prev;
            }
            Collections.reverse(path);
            answer = finalCost + "|" + String.join("->", path);
        }

        // d. Gửi GET request để nộp bài
        String submitUrl = String.format("%s/api/rest/path/submit?studentCode=%s&qCode=%s&requestId=%s&answer=%s",
                base, URLEncoder.encode(studentCode, StandardCharsets.UTF_8), URLEncoder.encode(qCode, StandardCharsets.UTF_8),
                URLEncoder.encode(requestId, StandardCharsets.UTF_8), URLEncoder.encode(answer, StandardCharsets.UTF_8));

        var submitReq = HttpRequest.newBuilder(URI.create(submitUrl)).GET().build();
        var postResponse = client.send(submitReq, HttpResponse.BodyHandlers.ofString());

        // e. In kết quả
        System.out.println("Response from server: " + postResponse.body());
    }

    private static int[] dijkstra(String startNode, Map<String, List<Node>> adj, List<String> points, Map<String, Integer> pointIndexMap) {
        Map<String, Integer> distances = new HashMap<>();
        adj.keySet().forEach(node -> distances.put(node, INF));
        distances.put(startNode, 0);

        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(startNode, 0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String u = current.name;
            if (current.cost > distances.get(u)) continue;

            if (adj.containsKey(u)) {
                for (Node neighbor : adj.get(u)) {
                    String v = neighbor.name;
                    int newDist = distances.get(u) + neighbor.cost;
                    if (newDist < distances.get(v)) {
                        distances.put(v, newDist);
                        pq.add(new Node(v, newDist));
                    }
                }
            }
        }

        int[] result = new int[points.size()];
        for (int i = 0; i < points.size(); i++) {
            result[i] = distances.getOrDefault(points.get(i), INF);
        }
        return result;
    }
}