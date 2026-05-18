package com.paymentgateway.payments.application;

public record PaymentCommandDispatchResult(Type type, String responseBodyJson) {

    public enum Type {
        ACCEPTED_202,
        REPLAYED_200
    }

    public static PaymentCommandDispatchResult accepted202(String responseBodyJson) {
        return new PaymentCommandDispatchResult(Type.ACCEPTED_202, responseBodyJson);
    }

    public static PaymentCommandDispatchResult replayed200(String responseBodyJson) {
        return new PaymentCommandDispatchResult(Type.REPLAYED_200, responseBodyJson);
    }
}
