package com.paymentgateway.payments.application;

import com.paymentgateway.payments.domain.exception.NoReceiptsForOrderException;
import com.paymentgateway.payments.domain.query.PaymentReceiptRecord;
import com.paymentgateway.payments.domain.repository.PaymentReceiptRepository;
import com.paymentgateway.payments.domain.value.CustomerId;
import com.paymentgateway.payments.domain.value.OrderId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentsQueryService {

    private static final Logger log = LoggerFactory.getLogger(PaymentsQueryService.class);

    private static final int DEFAULT_CUSTOMER_LIMIT = 50;

    private final PaymentReceiptRepository paymentReceiptRepository;

    public PaymentsQueryService(PaymentReceiptRepository paymentReceiptRepository) {
        this.paymentReceiptRepository = paymentReceiptRepository;
    }

    public List<PaymentReceiptRecord> listByOrder(OrderId orderId) {
        List<PaymentReceiptRecord> records = paymentReceiptRepository.findReceiptRecordsByOrderId(orderId);
        if (records.isEmpty()) {
            log.debug("No receipts found for orderId={}", orderId.value());
            throw new NoReceiptsForOrderException(orderId);
        }
        log.debug("Listed {} receipt(s) for orderId={}", records.size(), orderId.value());
        return records;
    }

    public List<PaymentReceiptRecord> listByCustomer(CustomerId customerId, Integer limit) {
        int effectiveLimit = limit == null ? DEFAULT_CUSTOMER_LIMIT : limit;
        List<PaymentReceiptRecord> records =
                paymentReceiptRepository.findReceiptRecordsByCustomerId(customerId, effectiveLimit);
        log.debug(
                "Listed {} receipt(s) for customerId={} limit={}",
                records.size(),
                customerId.value(),
                effectiveLimit);
        return records;
    }
}
