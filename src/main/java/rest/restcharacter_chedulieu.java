package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class restcharacter_chedulieu {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "AIFo6Lbs";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // 1. GET: Lấy chuỗi log
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/character?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        String responseBody = getResponse.body();
        System.out.println("DEBUG: Phản hồi từ server: " + responseBody);

        var res = mapper.readTree(responseBody);

        if (res.get("requestId") == null) {
            System.err.println("LỖI: Server không trả về 'requestId'. Vui lòng kiểm tra lại qCode và studentCode.");
            return;
        }

        String requestId = res.get("requestId").asText();
        String data = res.get("data").asText();

        // 2. LOGIC: Che dữ liệu bằng Regex
        // Regex cho email
        String emailRegex = "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b";
        // Regex cho SĐT Việt Nam 10 số, bắt đầu bằng 0
        String phoneRegex = "\\b0\\d{9}\\b";
        // Regex cho token=value (thay thế cả cụm)
        String tokenRegex = "token=[^\\s|]+";

        // Áp dụng thay thế lần lượt
        String maskedData = data.replaceAll(emailRegex, "[EMAIL]")
                .replaceAll(phoneRegex, "[PHONE]")
                .replaceAll(tokenRegex, "token=[TOKEN]");

        // 3. POST: Gửi kết quả đã che
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", maskedData // Câu trả lời là chuỗi đã được che
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/character/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        System.out.println("Kết quả POST: " + client.send(postReq, HttpResponse.BodyHandlers.ofString()).body());
    }
}