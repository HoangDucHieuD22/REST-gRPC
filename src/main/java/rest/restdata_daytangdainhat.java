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
import java.util.List;
import java.util.Map;

public class restdata_daytangdainhat {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "yxuNK2n0";       // Mã câu hỏi của bài này
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
        // The error indicates 'data' is an object. The array is nested within it.
        ArrayNode valuesNode = (ArrayNode) responseNode.get("data").get("values");
        List<Integer> values = new ArrayList<>();
        for (JsonNode value : valuesNode) {
            values.add(value.asInt());
        }

        // c. Tìm độ dài của dãy con tăng dài nhất (Longest Increasing Subsequence)
        int answer = lengthOfLIS(values);

        // d. Gửi POST request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        var postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        // e. In kết quả
        System.out.println("Response from server: " + postResponse.body());
    }

    /**
     * Tính độ dài của dãy con tăng dài nhất bằng thuật toán O(n log n).
     */
    private static int lengthOfLIS(List<Integer> nums) {
        if (nums == null || nums.isEmpty()) {
            return 0;
        }
        // 'tails' là một mảng lưu trữ phần tử cuối cùng nhỏ nhất
        // của tất cả các dãy con tăng có độ dài khác nhau.
        List<Integer> tails = new ArrayList<>();

        for (int num : nums) {
            // Sử dụng binary search để tìm vị trí chèn 'num'.
            // Nếu 'num' lớn hơn tất cả các phần tử trong 'tails', nó sẽ được thêm vào cuối,
            // mở rộng dãy con tăng dài nhất hiện tại.
            // Ngược lại, nó sẽ thay thế phần tử đầu tiên trong 'tails' mà lớn hơn hoặc bằng 'num'.
            // Điều này giúp tạo ra một dãy con tăng tiềm năng mới với phần tử cuối nhỏ hơn.
            int i = Collections.binarySearch(tails, num);
            if (i < 0) {
                i = -(i + 1);
            }
            if (i == tails.size()) {
                tails.add(num);
            } else {
                tails.set(i, num);
            }
        }
        return tails.size();
    }
}