package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class restobject_calculatefinalprice {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "yNHFr2RD";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // 1. GET: Lấy đối tượng sản phẩm
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/object?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        String responseBody = getResponse.body();
        System.out.println("DEBUG: Phản hồi từ server: " + responseBody);

        var res = mapper.readTree(responseBody);

        if (res.get("requestId") == null) {
            System.err.println("LỖI: Server không trả về 'requestId'. Vui lòng kiểm tra lại qCode và studentCode.");
            return;
        }

        String requestId = res.get("requestId").asText();
        JsonNode data = res.get("data"); // data là một object

        // 2. LOGIC: Tính giá cuối cùng bằng BigDecimal để đảm bảo độ chính xác
        BigDecimal price = new BigDecimal(data.get("price").asText());
        BigDecimal taxRate = new BigDecimal(data.get("taxRate").asText());
        BigDecimal discount = new BigDecimal(data.get("discount").asText());
        BigDecimal hundred = new BigDecimal("100");

        // finalPrice = price * (1 + taxRate / 100) * (1 - discount / 100)
        BigDecimal taxMultiplier = BigDecimal.ONE.add(taxRate.divide(hundred));
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discount.divide(hundred));

        BigDecimal finalPrice = price.multiply(taxMultiplier).multiply(discountMultiplier);

        // Làm tròn 2 chữ số thập phân theo yêu cầu
        finalPrice = finalPrice.setScale(2, RoundingMode.HALF_UP);

        // 3. POST: Gửi kết quả
        // Tạo object answer chỉ chứa finalPrice
        var answer = Map.of("finalPrice", finalPrice);

        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer // object lồng trong object
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/object/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        System.out.println("Kết quả POST: " + client.send(postReq, HttpResponse.BodyHandlers.ofString()).body());
    }
}