package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class restobject_chonbaogiatoiuutheophivaSLA {

    // Helper class to hold quote information along with its calculated fee
    static class CalculatedQuote {
        String carrier;
        double totalFee;
        int etaDays;
        double reliability;

        CalculatedQuote(JsonNode quoteNode, double weightKg) {
            this.carrier = quoteNode.get("carrier").asText();
            this.etaDays = quoteNode.get("etaDays").asInt();
            this.reliability = quoteNode.get("reliability").asDouble();
            // Correcting field names from snake_case to camelCase
            double baseFee = quoteNode.get("baseFee").asDouble();
            double perKgFee = quoteNode.get("perKgFee").asDouble();
            this.totalFee = baseFee + (perKgFee * weightKg);
        }
    }

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "SVR1snsk";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();
        var df = new DecimalFormat("0.00");

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/object?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        JsonNode dataNode = responseNode.get("data");
        double weightKg = dataNode.get("weightKg").asDouble();
        int maxEtaDays = dataNode.get("maxEtaDays").asInt();
        ArrayNode quotesNode = (ArrayNode) dataNode.get("quotes");

        // c. Lọc, tính phí và chọn quote tốt nhất
        Optional<CalculatedQuote> bestQuoteOpt = StreamSupport.stream(quotesNode.spliterator(), false)
                .map(quoteNode -> new CalculatedQuote(quoteNode, weightKg))
                .filter(cq -> cq.etaDays <= maxEtaDays) // Chỉ xét các quote đáp ứng SLA
                .min(Comparator.comparingDouble((CalculatedQuote cq) -> cq.totalFee) // Rẻ nhất
                        .thenComparing(Comparator.comparingDouble((CalculatedQuote cq) -> cq.reliability).reversed())); // Nếu hòa, reliability cao hơn

        // d. Chuẩn bị câu trả lời
        String answer;
        if (bestQuoteOpt.isPresent()) {
            CalculatedQuote bestQuote = bestQuoteOpt.get();
            answer = String.join("|",
                    bestQuote.carrier,
                    df.format(bestQuote.totalFee),
                    String.valueOf(bestQuote.etaDays)
            );
        } else {
            answer = ""; // Trường hợp không có quote nào phù hợp
        }

        // e. Gửi POST request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/object/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        var postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response from server: " + postResponse.body());
    }
}