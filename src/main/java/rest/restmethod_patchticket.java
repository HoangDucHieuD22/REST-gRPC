package rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class restmethod_patchticket {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "Va1MxI76";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET để nhận ticket
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/method?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        String responseBody = getResponse.body();
        System.out.println("DEBUG: Phản hồi từ GET: " + responseBody);

        var res = mapper.readTree(responseBody);

        if (res.get("requestId") == null) {
            System.err.println("LỖI: Server không trả về 'requestId'. Vui lòng kiểm tra lại qCode và studentCode.");
            return;
        }

        // b. Lấy requestId và etag từ body
        String requestId = res.get("requestId").asText();
        String etag = res.get("data").get("etag").asText();
        System.out.println("DEBUG: Đã nhận được etag: " + etag);

        // c. & d. Chuẩn bị và gửi request PATCH
        var answer = Map.of("status", "RESOLVED");
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer
        ));

        // Xây dựng request PATCH bằng cú pháp tường minh
        var patchReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/method/" + requestId))
                .header("Content-Type", "application/json")
                .header("If-Match", etag)
                // SỬ DỤNG CÚ PHÁP NÀY ĐỂ ĐẢM BẢO PHƯƠNG THỨC LÀ PATCH
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

        System.out.println("Đang gửi PATCH request tới: " + patchReq.uri().toString());
        System.out.println("Header If-Match: " + etag);
        System.out.println("Body của PATCH request: " + body);

        HttpResponse<String> patchResponse = client.send(patchReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("Kết quả cuối cùng từ PATCH: " + patchResponse.body());
    }
}