package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class restdata_trungvitruot {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "soF2hmED";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();
        var df = new DecimalFormat("#.00");

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        JsonNode dataNode = responseNode.get("data");
        ArrayNode valuesNode = (ArrayNode) dataNode.get("values");
        int windowSize = dataNode.get("windowSize").asInt();

        List<Integer> values = new ArrayList<>();
        for (JsonNode value : valuesNode) {
            values.add(value.asInt());
        }

        // c. Tính trung vị của từng cửa sổ trượt
        List<String> medians = new ArrayList<>();
        for (int i = 0; i <= values.size() - windowSize; i++) {
            List<Integer> window = new ArrayList<>(values.subList(i, i + windowSize));
            Collections.sort(window);
            double median;
            if (windowSize % 2 == 1) {
                median = window.get(windowSize / 2);
            } else {
                median = (window.get(windowSize / 2 - 1) + window.get(windowSize / 2)) / 2.0;
            }
            medians.add(df.format(median));
        }
        String answer = String.join(",", medians);

        // d. Gửi POST request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        var postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        // e. In kết quả
        System.out.println("Response from server: " + postResponse.body());
    }
}