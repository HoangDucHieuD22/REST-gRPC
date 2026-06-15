package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class restheader_chukyHMAC {

    // Hàm helper để tính HMAC-SHA256 và chuyển sang Hex
    private static String calculateHMAC(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        final String ALGORITHM = "HmacSHA256";
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Chuyển mảng byte sang chuỗi Hex
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "HLRN6ws2";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET để nhận dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/header?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        String responseBody = getResponse.body();
        System.out.println("DEBUG: Phản hồi từ GET: " + responseBody);

        var res = mapper.readTree(responseBody);

        if (res.get("requestId") == null) {
            System.err.println("LỖI: Server không trả về 'requestId'. Vui lòng kiểm tra lại qCode và studentCode.");
            return;
        }

        // b. Lấy các thông tin cần thiết từ body
        String requestId = res.get("requestId").asText();
        JsonNode dataNode = res.get("data");
        String nonce = dataNode.get("nonce").asText();
        String signingKey = dataNode.get("signingKey").asText();

        List<String> events = new ArrayList<>();
        for (JsonNode eventNode : dataNode.get("events")) {
            events.add(eventNode.asText());
        }

        // c. Tạo payload và tính chữ ký
        // Nối các event lại bằng dấu |
        String joinedEvents = String.join("|", events);
        // Tạo payload theo đúng định dạng
        String payload = String.format("%s:%s:%s", nonce, joinedEvents, studentCode.toUpperCase());
        System.out.println("DEBUG: Payload để ký: " + payload);

        // Tính chữ ký HMAC-SHA256
        String signature = calculateHMAC(signingKey, payload);
        System.out.println("DEBUG: Chữ ký đã tính (Hex): " + signature);

        // d. & e. Gửi POST với header X-Signature
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/header/submit"))
                .header("Content-Type", "application/json")
                // Thêm header chữ ký
                .header("X-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        System.out.println("Đang gửi POST request với header X-Signature: " + signature);

        HttpResponse<String> postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("Kết quả cuối cùng từ POST: " + postResponse.body());
    }
}
