package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class restpath_pathParameter {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "EG4AkgKL";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET đầu tiên để lấy danh sách sản phẩm
        var getReq1 = HttpRequest.newBuilder(URI.create(base + "/api/rest/path?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse1 = client.send(getReq1, HttpResponse.BodyHandlers.ofString());

        String responseBody1 = getResponse1.body();
        System.out.println("DEBUG: Phản hồi từ GET #1: " + responseBody1);

        var res1 = mapper.readTree(responseBody1);

        if (res1.get("requestId") == null) {
            System.err.println("LỖI: Server không trả về 'requestId'. Vui lòng kiểm tra lại qCode và studentCode.");
            return;
        }

        // b. Lấy requestId và danh sách sản phẩm
        String requestId = res1.get("requestId").asText();
        JsonNode products = res1.get("data");

        // c. Chọn một id từ danh sách (chọn cái đầu tiên cho đơn giản)
        if (!products.isArray() || products.isEmpty()) {
            System.err.println("LỖI: Server không trả về danh sách sản phẩm (data).");
            return;
        }
        String productId = products.get(0).get("id").asText();
        System.out.println("DEBUG: Đã chọn productId: " + productId);

        // d. & e. Xây dựng và gửi GET thứ hai
        // URL có dạng: /api/rest/path/{productId}?query_params...
        String url2 = String.format("%s/api/rest/path/%s?studentCode=%s&qCode=%s&requestId=%s&currency=USD",
                base, productId, studentCode, qCode, requestId);

        var getReq2 = HttpRequest.newBuilder(URI.create(url2)).build();

        System.out.println("Đang gửi GET request #2 tới: " + getReq2.uri());

        // Gửi request và nhận phản hồi cuối cùng
        HttpResponse<String> getResponse2 = client.send(getReq2, HttpResponse.BodyHandlers.ofString());

        System.out.println("Kết quả cuối cùng từ GET #2: " + getResponse2.body());
    }
}