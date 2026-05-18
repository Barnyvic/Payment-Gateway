package com.paymentgateway.payments.application;

import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.CustomerId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListPaymentsByCustomerService {

    private static final int DEFAULT_LIMIT = 50;

    private final PaymentReceiptRepository paymentReceiptRepository;

    public ListPaymentsByCustomerService(PaymentReceiptRepository paymentReceiptRepository) {
        this.paymentReceiptRepository = paymentReceiptRepository;
    }

    public List<PaymentReceiptRecord> listByCustomer(CustomerId customerId, Integer limit) {
        int effective = limit == null ? DEFAULT_LIMIT : limit;
        return paymentReceiptRepository.findReceiptRecordsByCustomerId(customerId, effective);
    }
}
