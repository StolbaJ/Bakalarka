package cz.cvut.fel.skiapp.backendconnection.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class IntegrationClientConfig {

    @Value("${api.bezky-servis.url}")
    private String bezkyServisUrl;

    @Value("${api.machine.url}")
    private String machineUrl;

    /**
     * Klient pro komunikaci s API kamaráda (BezkyServis).
     * Zde můžeme nastavit základní URL, abychom ji nemuseli všude psát.
     */
    @Bean
    public RestClient bezkyServisClient() {
        return RestClient.builder()
                .baseUrl(bezkyServisUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Klient pro ovládání stroje (Python simulátor).
     */
    @Bean
    public RestClient machineClient() {
        return RestClient.builder()
                .baseUrl(machineUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
