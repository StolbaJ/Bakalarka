/*
package cz.cvut.fel.skiapp.backendconnection.integration;

import cz.cvut.fel.skiapp.backendconnection.dto.ExternalOrderRequest;
import cz.cvut.fel.skiapp.backendconnection.model.ServiceOrder;
import cz.cvut.fel.skiapp.backendconnection.model.Ski;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class FrontendIntegrationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private String currentToken;

    @Value("${external.api.url}")
    private String baseUrl;

    @Value("${external.api.usernameFrontend}")
    private String username;

    @Value("${external.api.passwordFrontend}")
    private String password;

    */
/**
     * Provede přihlášení a získá JWT token.
     * Odpovídá endpointu POST /api/auth/login z dokumentace.
     *//*

    public void login() {
        String url = baseUrl + "/api/auth/login";
        Map<String, String> credentials = Map.of(
                "username", username,
                "password", password
        );

        try {
            var response = restTemplate.postForEntity(url, credentials, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                this.currentToken = (String) response.getBody().get("token");
                log.info("Úspěšně získán JWT token pro externí API.");
            }
        } catch (Exception e) {
            log.error("Chyba při přihlašování k externímu API: {}", e.getMessage());
        }
    }

    */
/**
     * Vytvoří zakázku v externím systému na základě lokální entity.
     * Implementuje mapování dat z bacWork na strukturu BezkyServis.
     *//*

    public void syncOrderToExternal(ServiceOrder localOrder) {
        if (currentToken == null) login();

        String url = baseUrl + "/api/technician/orders";

        // Příprava DTO (Data Transfer Object) podle readme dokumentace
        ExternalOrderRequest request = new ExternalOrderRequest();
        request.setCustomerId(localOrder.getCustomer().getExternalId());
        request.setPohodaId(localOrder.getPohodaId());

        // Mapování ID lyží a požadovaných struktur
        long[] skiIds = localOrder.getSkis().stream().mapToLong(Ski::getId).toArray();
        request.setSkiIds(skiIds);

        // Nastavení hlaviček s Bearer tokenem
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(currentToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ExternalOrderRequest> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
            log.info("Zakázka {} úspěšně synchronizována.", localOrder.getOrderCode());
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Token vypršel, zkouším re-login.");
            login(); // Rekurzivní pokus po obnovení tokenu
            syncOrderToExternal(localOrder);
        }
    }
}
*/
