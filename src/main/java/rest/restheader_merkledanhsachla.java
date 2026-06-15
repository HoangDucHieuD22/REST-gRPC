package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class restheader_merkledanhsachla {

    public static void main(String[] args) throws Exception {
        // --- KHAI BÁO ---
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String qCode = "jaKVvnPt";       // Mã câu hỏi của bài này
        String base = "http://36.50.135.242:2230";
        // ----------------

        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // a. Gửi GET request để lấy dữ liệu
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/header?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var getResponse = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        JsonNode responseNode = mapper.readTree(getResponse.body());
        String requestId = responseNode.get("requestId").asText();

        // b. Lấy dữ liệu từ response
        ArrayNode leavesNode = (ArrayNode) responseNode.get("data").get("leaves");
        List<String> leaves = StreamSupport.stream(leavesNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());

        // c. Xây dựng Merkle Tree và lấy root hash
        String answer = calculateMerkleRoot(leaves);

        // d. Gửi POST request để nộp bài
        var body = mapper.writeValueAsString(Map.of(
                "studentCode", studentCode,
                "qCode", qCode,
                "requestId", requestId,
                "answer", answer
        ));

        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/header/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        var postResponse = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        // e. In kết quả
        System.out.println("Response from server: " + postResponse.body());
    }

    /**
     * Tính toán Merkle root từ danh sách các lá.
     */
    private static String calculateMerkleRoot(List<String> leaves) throws Exception {
        if (leaves.isEmpty()) {
            return bytesToHex(sha256("".getBytes(StandardCharsets.UTF_8)));
        }

        // Băm tất cả các lá để tạo level đầu tiên
        List<byte[]> currentLevel = leaves.stream()
                .map(leaf -> sha256(leaf.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.toList());

        // Ghép cặp và băm lên các level trên cho đến khi còn lại root
        while (currentLevel.size() > 1) {
            List<byte[]> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                byte[] left = currentLevel.get(i);
                // Nếu số node lẻ, nhân đôi node cuối
                byte[] right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;

                // Ghép byte của hai hash con
                byte[] combined = new byte[left.length + right.length];
                System.arraycopy(left, 0, combined, 0, left.length);
                System.arraycopy(right, 0, combined, left.length, right.length);

                // Băm để tạo node cha
                nextLevel.add(sha256(combined));
            }
            currentLevel = nextLevel;
        }

        return bytesToHex(currentLevel.get(0));
    }

    /**
     * Băm một mảng byte bằng SHA-256.
     */
    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Chuyển một mảng byte thành chuỗi hex chữ thường.
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}