package cz.cvut.fel.skiapp.backendconnection.controller;

import cz.cvut.fel.skiapp.backendconnection.service.PohodaOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pohoda")
@Slf4j
public class PohodaImportController {

    private final PohodaOrderService orderService;

    // Tento klíč by měl být ideálně v application.properties nebo jako env proměnná
    private final String API_KEY = "moje-tajne-heslo-pro-import-123";

    public PohodaImportController(PohodaOrderService orderService) {
        this.orderService = orderService;
    }




    // --- EXPORT DAT DO POHODY ---
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> exportToPohoda(@RequestHeader("X-API-KEY") String providedApiKey) {
        if (!API_KEY.equals(providedApiKey)) return ResponseEntity.status(401).build();

        // Metoda v orderService najde v DB objednávky s flagem 'pripraveno_pro_pohodu'
        // a vygeneruje XML typu <requestPack> pro Pohodu
        String xmlExport = "aa"; //TODO = orderService.generatePohodaXmlForExport();

        if (xmlExport == null || xmlExport.isEmpty()) {
            return ResponseEntity.noContent().build(); // Nic k odeslání
        }

        return ResponseEntity.ok(xmlExport);
    }

    // Volitelný endpoint pro potvrzení, že skript XML do Pohody úspěšně doručil
    @PostMapping("/confirm-export")
    public ResponseEntity<Void> confirmExport(
            @RequestHeader("X-API-KEY") String providedApiKey,
            @RequestBody List<Long> orderIds) {
        if (!API_KEY.equals(providedApiKey)) return ResponseEntity.status(401).build();

        //TODO orderService.markAsSyncedInPohoda(orderIds);
        return ResponseEntity.ok().build();
    }
}
