package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class restmethod_chongguitrung {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "teioW6EJ";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/method?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        JsonNode dataNode = responseNode.get("data");
        int capacity = dataNode.get("capacity").asInt();
        ArrayNode requestsNode = (ArrayNode) dataNode.get("requests");

        // c. Xử lý logic chống gửi trùng bằng LRU cache
        // LinkedHashMap sẽ được dùng như một LRU cache cho các 'key'
        LinkedHashMap<String, Boolean> lruKeyCache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > capacity;
            }
        };
        
        List<String> acceptedRequestIds = new ArrayList<>();

        for (JsonNode requestNode : requestsNode) {
            String id = requestNode.get("id").asText();
            String key = requestNode.get("key").asText();

            // Một request được chấp nhận nếu key của nó KHÔNG có trong cache
            if (!lruKeyCache.containsKey(key)) {
                acceptedRequestIds.add(id);
            }
            
            // Dù request có được chấp nhận hay không, key của nó vẫn được "chạm" tới
            // và trở thành most-recently-used.
            lruKeyCache.put(key, true);
        }

        String answer = String.join(",", acceptedRequestIds);

        // d. Gửi PUT request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "answer", answer
        ));

        String putUrl = base + "/api/rest/method/" + requestId;
        var putReq = HttpRequest.newBuilder(URI.create(putUrl))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)).build();

        var putResponse = client.send(putReq, HttpResponse.BodyHandlers.ofString());

        // e. In kết quả
        System.out.println("Response from server: " + putResponse.body());
    }
}