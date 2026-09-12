package com.rental.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private boolean demoMode = false;

    private String appName = "house-rental-system";
}
