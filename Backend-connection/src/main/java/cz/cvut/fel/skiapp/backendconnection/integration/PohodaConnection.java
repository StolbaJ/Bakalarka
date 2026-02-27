package cz.cvut.fel.skiapp.backendconnection.integration;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;

public class PohodaConnection {


    private static final String POHODA_URL = "http://localhost:444/xml";
    private static final String AUTH_HEADER = "STW-Authorization";
    private static final String AUTH_VALUE = "tvoje_autentizace"; //Doplnit správnou autentizaci

    public ResponseEntity<String> getAllOrders() {
        String xmlRequest =
                "<dat:dataPack xmlns:dat=\"http://www.stormware.cz/schema/version_2/data.xsd\" xmlns:ftr=\"http://www.stormware.cz/schema/version_2/filter.xsd\" xmlns:lst=\"http://www.stormware.cz/schema/version_2/list.xsd\" xmlns:typ=\"http://www.stormware.cz/schema/version_2/type.xsd\" id=\"001\" ico=\"12345678\" application=\"StwTest\" version=\"2.0\" note=\"Požadavek na export výběru objednávek\">\n" +
                        "<dat:dataPackItem id=\"li1\" version=\"2.0\">\n" +
                        "<!--  export objednávek  -->\n" +
                        "<lst:listOrderRequest version=\"2.0\" orderType=\"receivedOrder\" orderVersion=\"2.0\">\n" +
                        "<lst:requestOrder>\n" +
                        "<ftr:filter>\n" +
                        "<!-- export vsech dokladu -->\n" +
                        "</ftr:filter>\n" +
                        "</lst:requestOrder>\n" +
                        "</lst:listOrderRequest>\n" +
                        "</dat:dataPackItem>\n" +
                        "</dat:dataPack>";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set(AUTH_HEADER, AUTH_VALUE);

        HttpEntity<String> request = new HttpEntity<>(xmlRequest, headers);

        return restTemplate.postForEntity(POHODA_URL, request, String.class);
    }

    public ResponseEntity<String> getNewOrders(Timestamp timestamp) {
        String xmlRequest =
                "<dat:dataPack xmlns:dat=\"http://www.stormware.cz/schema/version_2/data.xsd\" xmlns:ftr=\"http://www.stormware.cz/schema/version_2/filter.xsd\" xmlns:lst=\"http://www.stormware.cz/schema/version_2/list.xsd\" xmlns:typ=\"http://www.stormware.cz/schema/version_2/type.xsd\" id=\"001\" ico=\"12345678\" application=\"StwTest\" version=\"2.0\" note=\"Požadavek na export výběru objednávek\">\n" +
                        "<dat:dataPackItem id=\"li1\" version=\"2.0\">\n" +
                        "<!--  export objednávek  -->\n" +
                        "<lst:listOrderRequest version=\"2.0\" orderType=\"receivedOrder\" orderVersion=\"2.0\">\n" +
                        "<lst:requestOrder>\n" +
                        "<ftr:filter>\n" +
                        "<!-- export objednávek od určitého času -->\n" +
                        "<ftr:from>" + timestamp.toString() + "</ftr:from>\n" +
                        "</ftr:filter>\n" +
                        "</lst:requestOrder>\n" +
                        "</lst:listOrderRequest>\n" +
                        "</dat:dataPackItem>\n" +
                        "</dat:dataPack>";
        //TODO: Přidat správný timestamp pro získání pouze nových objednávek od posledního dotazu - správný formát
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set(AUTH_HEADER, AUTH_VALUE);

        HttpEntity<String> request = new HttpEntity<>(xmlRequest, headers);

        return restTemplate.postForEntity(POHODA_URL, request, String.class);
    }
}
