package com.attunedtechnology.verops.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Entry point for both stand-alone (java -jar) and WAR (Tomcat) deployment.
 *
 * <p>The WAR is named {@code verops.war} so Tomcat serves it under the
 * context path {@code /verops}.  The webhook URL becomes:
 * <pre>https://o11y.attunedtechnology.com/verops/demo/weekly-presence-report</pre>
 */
@SpringBootApplication
public class PresenceWebhookApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(PresenceWebhookApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(PresenceWebhookApplication.class, args);
    }
}
