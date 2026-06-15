package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class restmethod_giaiquyetphuthuoctheodouutien {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "oN1njWgC";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/method?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        JsonNode dataNode = responseNode.get("data");
        ArrayNode modulesNode = (ArrayNode) dataNode.get("modules");
        ArrayNode depsNode = (ArrayNode) dataNode.get("deps");

        // c. Sắp xếp topo với ưu tiên version cao hơn
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        Map<String, Integer> versions = new HashMap<>();

        // Khởi tạo đồ thị, in-degree và lưu versions
        for (JsonNode moduleNode : modulesNode) {
            String name = moduleNode.get("id").asText(); // Assuming 'id' from the error log
            int version = moduleNode.get("version").asInt();
            versions.put(name, version);
            inDegree.put(name, 0);
            adj.put(name, new ArrayList<>());
        }

        // Xây dựng đồ thị và tính in-degree, checking for "before" and "after"
        for (JsonNode depNode : depsNode) {
            JsonNode fromNode = depNode.get("before");
            JsonNode toNode = depNode.get("after");

            if (fromNode != null && toNode != null) {
                String prerequisite = fromNode.asText();
                String dependent = toNode.asText();
                if (adj.containsKey(prerequisite) && inDegree.containsKey(dependent)) {
                    adj.get(prerequisite).add(dependent);
                    inDegree.put(dependent, inDegree.get(dependent) + 1);
                }
            }
        }

        // PriorityQueue ưu tiên version cao hơn, nếu bằng nhau thì ưu tiên tên theo từ điển
        PriorityQueue<String> queue = new PriorityQueue<>(
                Comparator.comparing((String m) -> versions.get(m)).reversed()
                          .thenComparing(Comparator.naturalOrder())
        );

        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sortedModules = new ArrayList<>();
        while (!queue.isEmpty()) {
            String u = queue.poll();
            sortedModules.add(u);

            if (adj.containsKey(u)) {
                for (String v : adj.get(u)) {
                    inDegree.put(v, inDegree.get(v) - 1);
                    if (inDegree.get(v) == 0) {
                        queue.add(v);
                    }
                }
            }
        }

        // d. Chuẩn bị câu trả lời
        String answer = String.join(",", sortedModules);

        // e. Gửi PUT request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "answer", answer
        ));

        String putUrl = base + "/api/rest/method/" + requestId;
        var putReq = HttpRequest.newBuilder(URI.create(putUrl))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)).build();

        var putResponse = client.send(putReq, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response from server: " + putResponse.body());
    }
}