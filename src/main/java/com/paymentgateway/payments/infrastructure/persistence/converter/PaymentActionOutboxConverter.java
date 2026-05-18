package com.paymentgateway.payments.infrastructure.persistence.converter;

import com.paymentgateway.common.util.PaymentAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentActionOutboxConverter implements AttributeConverter<PaymentAction, String> {

    @Override
    public String convertToDatabaseColumn(PaymentAction attribute) {
        return attribute == null ? null : attribute.outboxEventTypeName();
    }

    @Override
    public PaymentAction convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PaymentAction.fromOutboxEventTypeName(dbData);
    }
}
