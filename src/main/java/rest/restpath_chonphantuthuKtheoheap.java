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
import java.util.Map;
import java.util.PriorityQueue;

public class restpath_chonphantuthuKtheoheap {

    // Helper class to represent a record, implementing Comparable for the heap
    static class Record implements Comparable<Record> {
        String id;
        int value;
        String type;

        Record(JsonNode node) {
            this.id = node.get("id").asText();
            this.value = node.get("value").asInt();
            this.type = node.get("type").asText();
        }

        @Override
        public int compareTo(Record other) {
            // Default comparison for max-heap (larger values have higher priority)
            return Integer.compare(other.value, this.value);
        }
    }

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "Qub1EXWo";       // Mã câu hỏi của bài này
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
        ArrayNode recordsNode = (ArrayNode) dataNode.get("records");
        int k = dataNode.get("k").asInt();
        String filterType = dataNode.get("type").asText();

        // c. Lọc theo type và chọn phần tử thứ k bằng heap
        PriorityQueue<Record> maxHeap = new PriorityQueue<>();
        for (JsonNode recordNode : recordsNode) {
            Record record = new Record(recordNode);
            if (record.type.equals(filterType)) {
                maxHeap.add(record);
            }
        }

        // Lấy ra phần tử thứ k
        Record kthLargest = null;
        if (k > 0 && k <= maxHeap.size()) {
            for (int i = 0; i < k; i++) {
                kthLargest = maxHeap.poll();
            }
        }

        // d. Chuẩn bị câu trả lời
        String answer = "";
        if (kthLargest != null) {
            answer = kthLargest.id + "|" + kthLargest.value;
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