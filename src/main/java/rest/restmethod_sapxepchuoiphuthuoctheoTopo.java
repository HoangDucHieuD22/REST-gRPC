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
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class restmethod_sapxepchuoiphuthuoctheoTopo {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "HMqQhfCa";       // Mã câu hỏi của bài này
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
        ArrayNode tasksNode = (ArrayNode) dataNode.get("tasks");
        ArrayNode depsNode = (ArrayNode) dataNode.get("deps");

        // c. Sắp xếp topo bằng thuật toán Kahn
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();

        // Khởi tạo đồ thị và in-degree
        for (JsonNode taskNode : tasksNode) {
            String task = taskNode.asText();
            inDegree.put(task, 0);
            adj.put(task, new ArrayList<>());
        }

        // Xây dựng đồ thị và tính in-degree từ các dependency
        // Correcting field names to the standard "from" and "to"
        for (JsonNode depNode : depsNode) {
            JsonNode fromNode = depNode.get("from");
            JsonNode toNode = depNode.get("to");

            if (fromNode != null && toNode != null) {
                String from = fromNode.asText();
                String to = toNode.asText();
                adj.get(from).add(to);
                inDegree.put(to, inDegree.get(to) + 1);
            }
        }

        // Sử dụng PriorityQueue để đảm bảo thứ tự từ điển khi có nhiều lựa chọn
        PriorityQueue<String> queue = new PriorityQueue<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sortedTasks = new ArrayList<>();
        while (!queue.isEmpty()) {
            String u = queue.poll();
            sortedTasks.add(u);

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
        String answer = String.join(",", sortedTasks);

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