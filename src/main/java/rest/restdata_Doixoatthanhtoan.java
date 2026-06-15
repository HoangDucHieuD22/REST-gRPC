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

public class restdata_Doixoatthanhtoan {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "ROpywvWX"; // Thay qCode đề bài cho
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // 1. GET: Lấy dữ liệu giao dịch
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var res = mapper.readTree(client.send(getReq, HttpResponse.BodyHandlers.ofString()).body());
        String requestId = res.get("requestId").asText();

        // 2. LOGIC: Tính toán các tổng
        BigDecimal capturedTotal = BigDecimal.ZERO;
        BigDecimal refundedTotal = BigDecimal.ZERO;
        int failedCount = 0;

        for (JsonNode transaction : res.get("data")) {
            String status = transaction.get("status").asText();
            BigDecimal amount = new BigDecimal(transaction.get("amount").asText());

            switch (status) {
                case "CAPTURED":
                    capturedTotal = capturedTotal.add(amount);
                    break;
                case "REFUNDED":
                    refundedTotal = refundedTotal.add(amount);
                    break;
                case "FAILED":
                    failedCount++;
                    break;
            }
        }

        BigDecimal netTotal = capturedTotal.subtract(refundedTotal);

        // Làm tròn 2 chữ số thập phân
        capturedTotal = capturedTotal.setScale(2, RoundingMode.HALF_UP);
        refundedTotal = refundedTotal.setScale(2, RoundingMode.HALF_UP);
        netTotal = netTotal.setScale(2, RoundingMode.HALF_UP);

        // 3. POST: Gửi kết quả
        var answer = Map.of(
                "capturedTotal", capturedTotal,
                "refundedTotal", refundedTotal,
                "netTotal", netTotal,
                "failedCount", failedCount
        );

        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer // object lồng object
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        System.out.println("Kết quả POST: " + client.send(postReq, HttpResponse.BodyHandlers.ofString()).body());
    }
}