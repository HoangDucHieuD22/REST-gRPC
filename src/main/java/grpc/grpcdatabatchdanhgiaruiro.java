package grpc;

import GRPC.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

public class grpcdatabatchdanhgiaruiro {

    public static void main(String[] args) {
        // --- KHAI BÁO ---
        String examIp = "36.50.135.242";
        int examPort = 2240;
        String studentCode = "B22DCVT192"; // Thay mã sinh viên
        String questionAlias = "YUyw4rl9";   // Mã câu hỏi của bài này
        // ----------------

        ManagedChannel channel = null;
        try {
            // 1. Tạo kết nối và stub
            channel = ManagedChannelBuilder.forAddress(examIp, examPort)
                    .usePlaintext()
                    .build();
            var stub = TypedJudgeServiceGrpc.newBlockingStub(channel);

            // a. Gửi request để lấy dữ liệu
            TypedJudgeRequest request = TypedJudgeRequest.newBuilder()
                    .setStudentCode(studentCode)
                    .setQuestionAlias(questionAlias)
                    .build();
            TypedJudgeResponse response = stub.requestTyped(request);
            String requestId = response.getRequestId();

            // b. Lấy danh sách giao dịch từ response
            List<TransactionRecord> transactions = response.getTransactionRiskBatch().getTransactionsList();

            // c. Lọc các giao dịch rủi ro cao
            List<TransactionRecord> highRiskTransactions = transactions.stream()
                    .filter(t -> t.getAmount() >= 5000 ||
                            t.getChargebackCount() >= 2 ||
                            (t.getNewDevice() && !t.getCountry().equals("VN")))
                    .collect(Collectors.toList());

            // d. Chuẩn bị câu trả lời
            List<String> highRiskIds = highRiskTransactions.stream()
                    .map(TransactionRecord::getTransactionId)
                    .collect(Collectors.toList());

            int reviewCount = highRiskIds.size();

            BigDecimal totalHighRiskAmount = highRiskTransactions.stream()
                    .map(t -> BigDecimal.valueOf(t.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double roundedTotalAmount = totalHighRiskAmount.setScale(2, RoundingMode.HALF_UP).doubleValue();

            TransactionRiskAnswer answer = TransactionRiskAnswer.newBuilder()
                    .addAllHighRiskTransactionIds(highRiskIds)
                    .setReviewCount(reviewCount)
                    .setTotalHighRiskAmount(roundedTotalAmount)
                    .build();

            // e. Gửi SubmitTyped request
            TypedSubmitRequest submitRequest = TypedSubmitRequest.newBuilder()
                    .setStudentCode(studentCode)
                    .setQuestionAlias(questionAlias)
                    .setRequestId(requestId)
                    .setTransactionRiskAnswer(answer)
                    .build();

            TypedSubmitResponse finalResponse = stub.submitTyped(submitRequest);

            System.out.println("Kết quả cuối cùng: " + finalResponse.getStatus());
            System.out.println("Message: " + finalResponse.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }
}