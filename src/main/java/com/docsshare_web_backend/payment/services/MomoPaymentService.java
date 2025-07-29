package com.docsshare_web_backend.payment.services;

import com.docsshare_web_backend.payment.dto.responses.MomoPaymentResponse;
import com.docsshare_web_backend.payment.enums.PaymentStatus;

public interface MomoPaymentService {

   /**
    * Tạo giao dịch thanh toán trên MoMo.
    * @param orderId ID của đơn hàng
    * @param amount Số tiền thanh toán (VNĐ)
    * @return Thông tin trả về từ MoMo (chứa payUrl, qrCodeUrl,...)
    */
   MomoPaymentResponse createPayment(Long orderId, Integer amount) throws Exception;

   /**
    * Cập nhật trạng thái thanh toán sau khi nhận callback IPN từ MoMo.
    * @param transactionId Mã giao dịch (orderId bên MoMo)
    * @param status Trạng thái thanh toán (SUCCESS, FAILED)
    */
   void updatePaymentStatus(String transactionId, PaymentStatus status);
}