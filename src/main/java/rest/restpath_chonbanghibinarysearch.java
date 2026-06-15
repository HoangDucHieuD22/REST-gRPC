package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class restpath_chonbanghibinarysearch {

    // Helper class to represent a record
    static class Record {
        String id;
        int value;

        Record(JsonNode node) {
            this.id = node.get("id").asText();
            this.value = node.get("threshold").asInt();
        }
    }

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "rRRmAEgT";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/path?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        JsonNode dataNode = responseNode.get("data");
        ArrayNode recordsNode = (ArrayNode) dataNode.get("records");
        int target = dataNode.get("target").asInt();

        List<Record> records = new ArrayList<>();
        for (JsonNode recordNode : recordsNode) {
            records.add(new Record(recordNode));
        }

        // c. Dùng binary search để tìm bản ghi phù hợp
        String answer = "";
        int low = 0;
        int high = records.size() - 1;
        int resultIndex = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (records.get(mid).value >= target) {
                resultIndex = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        if (resultIndex != -1) {
            answer = records.get(resultIndex).id;
        }

        // d. Gửi GET request để nộp bài (Server yêu cầu GET, không phải POST)
        String submitUrl = String.format("%s/api/rest/path/submit?studentCode=%s&qCode=%s&requestId=%s&answer=%s",
                base,
                URLEncoder.encode(studentCode, StandardCharsets.UTF_8),
                URLEncoder.encode(qCode, StandardCharsets.UTF_8),
                URLEncoder.encode(requestId, StandardCharsets.UTF_8),
                URLEncoder.encode(answer, StandardCharsets.UTF_8)
        );

        var submitReq = HttpRequest.newBuilder(URI.create(submitUrl))
                .GET()
                .build();

        var postResponse = client.send(submitReq, HttpResponse.BodyHandlers.ofString());

        // e. In kết quả
        System.out.println("Response from server: " + postResponse.body());
    }
}