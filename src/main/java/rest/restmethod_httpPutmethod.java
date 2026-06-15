package rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class restmethod_httpPutmethod {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "1fQSntsI";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET để nhận task
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/method?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        String responseBody = getResponse.body();
        System.out.println("DEBUG: Phản hồi từ GET: " + responseBody);

        var res = mapper.readTree(responseBody);

        if (res.get("requestId") == null) {
            System.err.println("LỖI: Server không trả về 'requestId'. Vui lòng kiểm tra lại qCode và studentCode.");
            return;
        }

        // b. Lấy requestId từ server
        String requestId = res.get("requestId").asText();

        // c. & d. Chuẩn bị và gửi request PUT
        // Tạo object answer
        var answer = Map.of("status", "done");

        // Tạo body JSON hoàn chỉnh
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "answer", answer
        ));

        // Xây dựng request PUT
        // URL cho request PUT có chứa requestId
        var putReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/method/" + requestId))
                .header("Content-Type", "application/json")
                // Sử dụng phương thức PUT()
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        System.out.println("Đang gửi PUT request tới: " + putReq.uri());
        System.out.println("Body của PUT request: " + body);

        // Gửi request và nhận phản hồi
        HttpResponse<String> putResponse = client.send(putReq, HttpResponse.BodyHandlers.ofString());

        System.out.println("Kết quả cuối cùng từ PUT: " + putResponse.body());
    }
}