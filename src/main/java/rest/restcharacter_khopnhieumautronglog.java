package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class restcharacter_khopnhieumautronglog {

    // Inner class for the Aho-Corasick Trie Node
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        TrieNode failureLink = null;
        List<String> output = new ArrayList<>();
    }

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "5WoPzwa6";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/character?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        JsonNode dataNode = responseNode.get("data");
        String text = dataNode.get("text").asText();
        ArrayNode patternsNode = (ArrayNode) dataNode.get("patterns");
        List<String> patterns = StreamSupport.stream(patternsNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());

        // c. Đếm số lần xuất hiện bằng Aho-Corasick
        Map<String, Integer> counts = ahoCorasickSearch(text, patterns);

        // d. Format the answer string, preserving the original order of patterns
        String answer = patterns.stream()
                .map(p -> p + "=" + counts.getOrDefault(p, 0))
                .collect(Collectors.joining("|"));

        // e. Gửi POST request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/character/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        var postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response from server: " + postResponse.body());
    }

    private static TrieNode buildTrie(List<String> patterns) {
        TrieNode root = new TrieNode();
        for (String p : patterns) {
            if (p == null || p.isEmpty()) continue;
            TrieNode node = root;
            for (char ch : p.toCharArray()) {
                node = node.children.computeIfAbsent(ch, k -> new TrieNode());
            }
            node.output.add(p);
        }
        return root;
    }

    private static void buildFailureLinks(TrieNode root) {
        Queue<TrieNode> queue = new LinkedList<>();
        root.failureLink = root;

        for (TrieNode child : root.children.values()) {
            child.failureLink = root;
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            TrieNode node = queue.poll();
            for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
                char ch = entry.getKey();
                TrieNode child = entry.getValue();
                queue.add(child);

                TrieNode failure = node.failureLink;
                while (!failure.children.containsKey(ch) && failure != root) {
                    failure = failure.failureLink;
                }

                if (failure.children.containsKey(ch)) {
                    child.failureLink = failure.children.get(ch);
                } else {
                    child.failureLink = root;
                }
                child.output.addAll(child.failureLink.output);
            }
        }
    }

    private static Map<String, Integer> ahoCorasickSearch(String text, List<String> patterns) {
        Map<String, Integer> counts = new HashMap<>();
        patterns.forEach(p -> counts.put(p, 0));
        if (patterns.isEmpty()) return counts;

        TrieNode root = buildTrie(patterns);
        buildFailureLinks(root);

        TrieNode node = root;
        for (char ch : text.toCharArray()) {
            while (!node.children.containsKey(ch) && node != root) {
                node = node.failureLink;
            }
            if (node.children.containsKey(ch)) {
                node = node.children.get(ch);
            }

            if (!node.output.isEmpty()) {
                for (String p : node.output) {
                    counts.put(p, counts.get(p) + 1);
                }
            }
        }
        return counts;
    }
}