package cz.cvut.fel.skiapp.backendconnection.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetOrderDto;
import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetOrdersRoot;
import cz.cvut.fel.skiapp.backendconnection.mapper.ShoptetMapper;
import cz.cvut.fel.skiapp.backendconnection.model.Customer;
import cz.cvut.fel.skiapp.backendconnection.model.ServiceOrder;
import cz.cvut.fel.skiapp.backendconnection.model.Ski;
import cz.cvut.fel.skiapp.backendconnection.repository.CustomerRepository;
import cz.cvut.fel.skiapp.backendconnection.repository.ServiceOrderRepository;
import cz.cvut.fel.skiapp.backendconnection.repository.SkiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShoptetImportService {

    private final ServiceOrderRepository orderRepository;
    private final XmlMapper xmlMapper;
    private final ShoptetMapper shoptetMapper;
    private final CustomerRepository customerRepository;
    private final SkiRepository skiRepository;

    private static final List<String> SERVICE_SKU_CODES = List.of("RC_018", "RC_081", "RC_084", "TEPLOUS230V");

    @Scheduled(cron = "0 * * * * *")  //"0 0/30 * * * *"
    public void importOrdersFromShoptet() {
        String url = "https://www.xcsport.cz/export/orders.xml?patternId=71&dateFrom=2026-3-28&partnerId=6&dateUntil=2026-3-29&hash=7c437cbe37b004d210e82f3088ce056d1024c9beec9e4b1e0c478179bcf7da8e";
        //String url = "https://vas-eshop.cz/export/orders.xml";
        log.debug("Starting import from Shoptet: {}", url);
        try {
            ShoptetOrdersRoot root = xmlMapper.readValue(new URL(url), ShoptetOrdersRoot.class);
            if (root.getOrders() == null) return;

            for (ShoptetOrderDto dto : root.getOrders()) {
                if (containsServiceItem(dto) && !orderRepository.existsByShoptetId(dto.getShoptetId())) {

                    // ZÍSKÁNÍ ZÁKAZNÍKA
                    Customer customer = getOrCreateCustomer(dto);

                    // MAPOVÁNÍ
                    ServiceOrder entity = shoptetMapper.toEntity(dto, customer);

                    // PŘIPOJENÍ LYŽÍ
                    Ski temporarySki = new Ski("Temporary Ski - " + dto.getOrderCode());
                    skiRepository.save(temporarySki);
                    entity.getItems().forEach(item -> item.setSki(temporarySki));

                    // ULOŽENÍ
                    orderRepository.save(entity);

                    log.info("Importována zakázka {} pro: {}", dto.getOrderCode(), customer.getName());
                }
            }
        } catch (IOException e) {
            log.error("Error while importing: {}", e.getMessage());
        }
    }

    private Customer getOrCreateCustomer(ShoptetOrderDto dto) {
        // Zkusíme najít podle emailu v naší DB
        return customerRepository.findByEmail(dto.getCustomerEmail())
                .orElseGet(() -> {
                    // Pokud neexistuje, vytvoříme úplně novou entitu Customer
                    log.info("Nový zákazník detekován, ukládám: {}", dto.getCustomerEmail());
                    Customer newCustomer = Customer.builder()
                            .name(dto.getCustomerFullName())
                            .email(dto.getCustomerEmail())
                            .phone(dto.getCustomerPhone())
                            .address(dto.getCustomerAdress())
                            .build();
                    return customerRepository.save(newCustomer);
                });
    }

    private boolean containsServiceItem(ShoptetOrderDto dto) {
        if (dto.getItems() == null) {
            return false;
        }
        return dto.getItems().stream()
                .anyMatch(i -> SERVICE_SKU_CODES.contains(i.getProductCode()));
    }
}
