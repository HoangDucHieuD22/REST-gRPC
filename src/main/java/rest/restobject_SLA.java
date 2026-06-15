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

public class restobject_SLA {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "2PPQAXE7";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // 1. GET: Lấy dữ liệu đơn hàng và các báo giá
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
        JsonNode data = res.get("data");

        // 2. LOGIC: Tìm báo giá tốt nhất
        BigDecimal weightKg = new BigDecimal(data.get("weightKg").asText());
        int maxEtaDays = data.get("maxEtaDays").asInt();

        JsonNode bestQuote = null;
        BigDecimal bestTotalFee = new BigDecimal(Double.MAX_VALUE); // Khởi tạo với giá trị lớn nhất
        double bestReliability = -1.0; // Khởi tạo với giá trị nhỏ nhất

        for (JsonNode quote : data.get("quotes")) {
            int etaDays = quote.get("etaDays").asInt();

            // c. Chỉ xét quote có etaDays <= maxEtaDays
            if (etaDays <= maxEtaDays) {
                BigDecimal baseFee = new BigDecimal(quote.get("baseFee").asText());
                BigDecimal perKgFee = new BigDecimal(quote.get("perKgFee").asText());

                // Tính totalFee và làm tròn 2 chữ số
                BigDecimal totalFee = baseFee.add(weightKg.multiply(perKgFee))
                        .setScale(2, RoundingMode.HALF_UP);

                // d. Chọn quote tốt nhất
                // Ưu tiên 1: totalFee nhỏ hơn
                if (totalFee.compareTo(bestTotalFee) < 0) {
                    bestTotalFee = totalFee;
                    bestReliability = quote.get("reliability").asDouble();
                    bestQuote = quote;
                }
                // Ưu tiên 2: nếu totalFee bằng nhau, chọn reliability cao hơn
                else if (totalFee.compareTo(bestTotalFee) == 0) {
                    double currentReliability = quote.get("reliability").asDouble();
                    if (currentReliability > bestReliability) {
                        bestReliability = currentReliability;
                        bestQuote = quote;
                        // bestTotalFee không đổi vì chúng bằng nhau
                    }
                }
            }
        }

        // 3. POST: Gửi kết quả
        if (bestQuote == null) {
            System.err.println("LỖI: Không tìm thấy báo giá nào phù hợp.");
            return;
        }

        // e. Tạo object answer từ bestQuote đã chọn
        var answer = Map.of(
                "carrier", bestQuote.get("carrier").asText(),
                "totalFee", bestTotalFee,
                "etaDays", bestQuote.get("etaDays").asInt()
        );

        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/object/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        System.out.println("Kết quả POST: " + client.send(postReq, HttpResponse.BodyHandlers.ofString()).body());
    }
}