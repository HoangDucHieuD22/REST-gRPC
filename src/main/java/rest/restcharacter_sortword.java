package rest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;
public class restcharacter_sortword {
    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "vD6LEkjT";      // Thay qCode đề bài cho
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // 1. GET: Lấy chuỗi dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/character?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        String responseBody = getResponse.body();
        // IN RA ĐỂ DEBUG: Luôn kiểm tra xem server trả về gì
        System.out.println("DEBUG: Phản hồi từ server: " + responseBody);

        var res = mapper.readTree(responseBody);

        // KIỂM TRA PHẢN HỒI TRƯỚC KHI XỬ LÝ
        if (res.get("requestId") == null) {
            System.err.println("LỖI: Server không trả về 'requestId'. Vui lòng kiểm tra lại qCode và studentCode.");
            return; // Dừng chương trình
        }

        String requestId = res.get("requestId").asText();
        String data = res.get("data").asText();

        // 2. LOGIC: Tách, sắp xếp, và nối chuỗi
        // Tách chuỗi thành mảng các từ, dùng "\\s+" để xử lý tốt trường hợp có nhiều dấu cách
        String[] words = data.split("\\s+");

        // Sắp xếp mảng theo thứ tự từ điển (phân biệt hoa thường)
        Arrays.sort(words);

        // Nối các từ đã sắp xếp lại bằng một dấu cách
        String sortedString = String.join(" ", words);

        // 3. POST: Gửi kết quả
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", sortedString // Câu trả lời là chuỗi đã sắp xếp
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/character/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        System.out.println("Kết quả POST: " + client.send(postReq, HttpResponse.BodyHandlers.ofString()).body());
    }
}
