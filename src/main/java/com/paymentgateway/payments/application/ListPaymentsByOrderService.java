package com.paymentgateway.payments.application;

import com.paymentgateway.payments.domain.exception.NoReceiptsForOrderException;
import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.OrderId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListPaymentsByOrderService {

    private final PaymentReceiptRepository paymentReceiptRepository;

    public ListPaymentsByOrderService(PaymentReceiptRepository paymentReceiptRepository) {
        this.paymentReceiptRepository = paymentReceiptRepository;
    }

    public List<PaymentReceiptRecord> listByOrder(OrderId orderId) {
        List<PaymentReceiptRecord> records = paymentReceiptRepository.findReceiptRecordsByOrderId(orderId);
        if (records.isEmpty()) {
            throw new NoReceiptsForOrderException(orderId);
        }
        return records;
    }
}
