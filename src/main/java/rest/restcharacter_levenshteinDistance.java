package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class restcharacter_levenshteinDistance {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "Q5Xe39ol";       // Mã câu hỏi của bài này
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
        String source = dataNode.get("source").asText();
        String target = dataNode.get("target").asText();

        // c. Tính khoảng cách Levenshtein
        int answer = calculateLevenshteinDistance(source, target);

        // d. Gửi POST request để nộp bài
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

        // e. In kết quả
        System.out.println("Response from server: " + postResponse.body());
    }

    /**
     * Tính khoảng cách Levenshtein (edit distance) giữa hai chuỗi bằng dynamic programming.
     */
    private static int calculateLevenshteinDistance(String source, String target) {
        int m = source.length();
        int n = target.length();

        // dp[i][j] sẽ lưu khoảng cách Levenshtein giữa i ký tự đầu của source
        // và j ký tự đầu của target.
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                // Nếu một trong hai chuỗi rỗng, khoảng cách bằng độ dài chuỗi còn lại
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    // Nếu ký tự cuối cùng giống nhau, không cần thao tác
                    int cost = (source.charAt(i - 1) == target.charAt(j - 1)) ? 0 : 1;

                    // Ngược lại, lấy giá trị nhỏ nhất của ba thao tác:
                    // 1. Chèn (dp[i][j-1])
                    // 2. Xóa (dp[i-1][j])
                    // 3. Thay thế (dp[i-1][j-1])
                    dp[i][j] = Math.min(Math.min(dp[i][j - 1] + 1, dp[i - 1][j] + 1), dp[i - 1][j - 1] + cost);
                }
            }
        }
        return dp[m][n];
    }
}