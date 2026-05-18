package com.paymentgateway.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataSanitizerTest {

    @Test
    void sanitize_masksPanAndCvvInJson() {
        String raw =
                "authorize card=4111111111111111 body={\"cvv\":\"123\",\"cardNumber\":\"4111111111111111\"}";
        String sanitized = SensitiveDataSanitizer.sanitize(raw);
        assertThat(sanitized).doesNotContain("4111111111111111");
        assertThat(sanitized).contains("************1111");
        assertThat(sanitized).contains("\"cvv\":\"***\"");
    }
}
