package com.paymentgateway.payments.application;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestHasher {

    private static final JsonMapper HASH_MAPPER =
            JsonMapper.builder().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true).build();

    public String sha256Hex(Object canonicalizablePayload) {
        try {
            String json = HASH_MAPPER.writeValueAsString(canonicalizablePayload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash request payload", e);
        }
    }
}
