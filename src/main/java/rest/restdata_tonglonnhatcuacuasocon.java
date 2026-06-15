package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class restdata_tonglonnhatcuacuasocon {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "KyCFOp00";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        JsonNode dataNode = responseNode.get("data");
        ArrayNode valuesNode = (ArrayNode) dataNode.get("values");
        int k = dataNode.get("k").asInt();
        int n = valuesNode.size();

        // c. Tìm tổng lớn nhất bằng prefix sum và deque
        long[] prefixSum = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + valuesNode.get(i).asInt();
        }

        long maxSum = Long.MIN_VALUE;
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addLast(0);

        for (int i = 1; i <= n; i++) {
            // Loại bỏ các index ngoài cửa sổ k
            if (!deque.isEmpty() && deque.peekFirst() < i - k) {
                deque.pollFirst();
            }

            // Cập nhật maxSum
            if (!deque.isEmpty()) {
                maxSum = Math.max(maxSum, prefixSum[i] - prefixSum[deque.peekFirst()]);
            }

            // Duy trì deque tăng dần (theo giá trị prefixSum)
            while (!deque.isEmpty() && prefixSum[deque.peekLast()] >= prefixSum[i]) {
                deque.pollLast();
            }
            deque.addLast(i);
        }

        // d. Gửi POST request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", maxSum
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        var postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        // e. In kết quả
        System.out.println("Response from server: " + postResponse.body());
    }
}