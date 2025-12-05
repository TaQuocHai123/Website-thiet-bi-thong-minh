package com.project.DuAnTotNghiep.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@Configuration
public class VNPayPropertiesBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(VNPayPropertiesBootstrap.class);

    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {
        // Keys we support
        String[] keys = new String[] { "vnpay.vnp_PayUrl", "vnpay.vnp_ReturnUrl", "vnpay.vnp_TmnCode",
                "vnpay.secretKey", "vnpay.vnp_ApiUrl", "vnpay.vnp_NotifyUrl" };

        for (String key : keys) {
            String value = env.getProperty(key);
            if (value != null && !value.isEmpty()) {
                // If a System property or Env var already exists, leave it; otherwise set the
                // System prop from application.properties
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    logger.info("VNPayPropertiesBootstrap: Set system property {} from application.properties", key);
                } else {
                    logger.debug(
                            "VNPayPropertiesBootstrap: System property {} already set; ignoring application.property value",
                            key);
                }

                // Also set VNP_* environment-style names if not set; this ensures ConfigVNPay's
                // env checks detect it
                String envName = key.toUpperCase().replace('.', '_');
                if (System.getenv(envName) == null) {
                    // We cannot set OS Environment variables at runtime portably from within Java;
                    // log so admins know
                    logger.debug("VNPayPropertiesBootstrap: No env var {}; app will use system properties", envName);
                }
            }
        }
    }
}
