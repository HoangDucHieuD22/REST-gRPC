package grpc;

import io.grpc.ManagedChannelBuilder;
// Import các class từ package GRPC đã được sinh ra
import GRPC.*;

import java.util.Arrays;

public class grpcdata1 {

    public static void main(String[] args) {
        // --- PHẦN KHAI BÁO CẦN THAY ĐỔI ---
        String examIp = "36.50.135.242";
        int examPort = 2240;
        String studentCode = "B22DCVT192";
        String questionAlias = "MGR4dmY6";
        // ------------------------------------

        // 1. Tạo kết nối và stub
        var channel = ManagedChannelBuilder.forAddress(examIp, examPort).usePlaintext().build();
        var stub = JudgeServiceGrpc.newBlockingStub(channel);

        // 2. Lấy dữ liệu (Sử dụng JudgeRequest)
        var request = JudgeRequest.newBuilder()
                .setStudentCode(studentCode)
                .setQuestionAlias(questionAlias)
                .build();
        // Biến response bây giờ sẽ có kiểu là JudgeResponse
        var response = stub.request(request);

        // 3. Xử lý logic (Ví dụ: tính tổng)
        int sum = Arrays.stream(response.getData().split(","))
                .mapToInt(s -> Integer.parseInt(s.trim()))
                .sum();

        // 4. Nộp bài (Sử dụng SubmitRequest)
        var submitRequest = SubmitRequest.newBuilder()
                .setStudentCode(studentCode)
                .setQuestionAlias(questionAlias)
                .setRequestId(response.getRequestId())
                .setAnswer(String.valueOf(sum))
                .build();
        var finalResponse = stub.submit(submitRequest);

        System.out.println("Kết quả: " + finalResponse.getStatus());

        channel.shutdown();
    }
}