package rest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class restdata_SumofInteger {
    public static void main(String[] args) throws Exception {
        String studentCode = "B22DCVT192", qCode = "9USw4RlV", base = "http://36.50.135.242:2230";
        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();

        // 1. GET
        var getReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data?studentCode=" + studentCode + "&qCode=" + qCode)).build();
        var res = mapper.readTree(client.send(getReq, HttpResponse.BodyHandlers.ofString()).body());
        
        // 2. Tính tổng
        int sum = 0;
        for (JsonNode n : res.get("data")) sum += n.asInt();
        
        // 3. POST
        var body = mapper.writeValueAsString(Map.of(
            "studentCode", studentCode, 
            "qCode", qCode, 
            "requestId", res.get("requestId").asText(), 
            "answer", sum
        ));
        
        var postReq = HttpRequest.newBuilder(URI.create(base + "/api/rest/data/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                
        System.out.println("Kết quả POST: " + client.send(postReq, HttpResponse.BodyHandlers.ofString()).body());
    }
}