package rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class restheader_headersignaturereplay {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "ULSgL7hF";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET để nhận dữ liệu và header
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/header?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        // b. & c. Đọc requestId từ body và X-Checksum từ header
        String responseBody = getResponse.body();
        System.out.println("DEBUG: Phản hồi từ GET: " + responseBody);

        // Đọc header X-Checksum. firstValue trả về Optional, dùng orElse để xử lý nếu không có
        String checksum = getResponse.headers().firstValue("X-Checksum").orElse("NOT_FOUND");
        System.out.println("DEBUG: Đọc được header X-Checksum: " + checksum);

        // Kiểm tra lỗi
        if (checksum.equals("NOT_FOUND")) {
            System.err.println("LỖI: Server không trả về header X-Checksum.");
            return;
        }

        var res = mapper.readTree(responseBody);
        String requestId = res.get("requestId").asText();

        // d. & e. Chuẩn bị và gửi POST request với header đã nhận
        // Body lần này không có trường "answer"
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId
        ));

        // Xây dựng request POST
        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/header/submit"))
                // Header mặc định cho JSON
                .header("Content-Type", "application/json")
                // Thêm header X-Checksum đã nhận được
                .header("X-Checksum", checksum)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        System.out.println("Đang gửi POST request với header X-Checksum: " + checksum);

        // Gửi request và nhận phản hồi cuối cùng
        HttpResponse<String> postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        System.out.println("Kết quả cuối cùng từ POST: " + postResponse.body());
    }
}