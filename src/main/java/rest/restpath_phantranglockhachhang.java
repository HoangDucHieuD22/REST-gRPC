package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class restpath_phantranglockhachhang {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "YXRrwQ5f";         // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET đầu tiên để lấy danh sách khách hàng
        var getReq1 = HttpRequest.newBuilder(URI.create(base + "/api/rest/path?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        HttpResponse<String> getResponse1 = client.send(getReq1, HttpResponse.BodyHandlers.ofString());

        String responseBody1 = getResponse1.body();
        System.out.println("DEBUG: Phản hồi từ GET #1: " + responseBody1);

        var res1 = mapper.readTree(responseBody1);

        if (res1.get("requestId") == null) {
            System.err.println("LỖI: Server không trả về 'requestId'. Vui lòng kiểm tra lại qCode và studentCode.");
            return;
        }

        // b. Lấy requestId và danh sách khách hàng
        String requestId = res1.get("requestId").asText();
        JsonNode customers = res1.get("data");

        // c. Tìm khách hàng có status OVERDUE và overdueAmount lớn nhất
        JsonNode bestCustomer = null;
        BigDecimal maxOverdueAmount = new BigDecimal("-1"); // Khởi tạo với giá trị âm để đảm bảo khách hàng đầu tiên luôn được chọn

        for (JsonNode customer : customers) {
            String status = customer.get("status").asText();
            if ("OVERDUE".equals(status)) {
                BigDecimal currentAmount = new BigDecimal(customer.get("overdueAmount").asText());
                // Nếu số tiền nợ hiện tại lớn hơn số tiền lớn nhất đã lưu
                if (currentAmount.compareTo(maxOverdueAmount) > 0) {
                    maxOverdueAmount = currentAmount;
                    bestCustomer = customer; // Lưu lại toàn bộ object của khách hàng tốt nhất
                }
            }
        }

        // d. & e. Xây dựng và gửi GET thứ hai
        if (bestCustomer == null) {
            System.err.println("LỖI: Không tìm thấy khách hàng nào có status OVERDUE.");
            return;
        }

        // Lấy các thông tin cần thiết từ khách hàng đã chọn
        String customerId = bestCustomer.get("customerId").asText();
        String status = bestCustomer.get("status").asText();
        int page = bestCustomer.get("page").asInt();

        System.out.println("DEBUG: Đã chọn customerId: " + customerId + " với page: " + page);

        // URL có dạng: /api/rest/path/{customerId}?query_params...
        String url2 = String.format("%s/api/rest/path/%s?studentCode=%s&qCode=%s&requestId=%s&status=%s&page=%d",
                base, customerId, studentCode, qCode, requestId, status, page);

        var getReq2 = HttpRequest.newBuilder(URI.create(url2)).build();

        System.out.println("Đang gửi GET request #2 tới: " + getReq2.uri());

        // Gửi request và nhận phản hồi cuối cùng
        HttpResponse<String> getResponse2 = client.send(getReq2, HttpResponse.BodyHandlers.ofString());

        System.out.println("Kết quả cuối cùng từ GET #2: " + getResponse2.body());
    }
}