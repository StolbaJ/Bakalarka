package cz.cvut.fel.skiapp.backendconnection.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Oprava importu
import cz.cvut.fel.skiapp.backendconnection.dto.ItemDto;
import cz.cvut.fel.skiapp.backendconnection.dto.OrderDto;
import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetOrdersRoot;
import cz.cvut.fel.skiapp.backendconnection.model.ShoptetOrder;
import cz.cvut.fel.skiapp.backendconnection.repository.ShoptetOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL; // Oprava z DocFlavor.URL na java.net.URL
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class ShoptetImportService {

    private final ShoptetOrderRepository shoptetOrderRepository;
    private final XmlMapper xmlMapper;

    // Definice kódů produktů, které považujeme za servis lyží
    private static final List<String> SERVICE_SKU_CODES = Arrays.asList("SERV-001", "SERV-002", "BRUS-01");

    public ShoptetImportService(ShoptetOrderRepository orderRepository) {
        this.shoptetOrderRepository = orderRepository;
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.xmlMapper.registerModule(new JavaTimeModule());
    }

    @Scheduled(cron = "0 0/30 * * * *")
    public void importOrdersFromShoptet() {
        String shoptetXmlUrl = "https://vas-eshop.cz/export/orders.xml?hash=vas-unikatni-hash";

        try {
            // Správné použití java.net.URL
            ShoptetOrdersRoot data = xmlMapper.readValue(new URL(shoptetXmlUrl), ShoptetOrdersRoot.class);

            if (data.getOrders() == null) return;

            for (OrderDto dto : data.getOrders()) {
                // 1. Filtrace: Obsahuje objednávka alespoň jeden servisní kód?
                if (containsServiceItem(dto)) {

                    // 2. Idempotence: Už jsme ji importovali dříve?
                    if (!shoptetOrderRepository.existsByShoptetId((dto.getShoptetId()))) {
                        saveOrder(dto);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Chyba při stahování dat ze Shoptetu: " + e.getMessage());
        }
    }

    /**
     * Metoda projde položky objednávky a vrátí true, pokud najde kód odpovídající servisu.
     */
    private boolean containsServiceItem(OrderDto dto) {
        if (dto.getItems() == null) return false;

        return dto.getItems().stream()
                .anyMatch(item -> SERVICE_SKU_CODES.contains(item.getProductCode()));
    }

    private void saveOrder(OrderDto dto) {
        // Zde vytvoříš instanci entity a namapuješ data z DTO
        /*ShoptetOrder order = new ShoptetOrder();
        order.setShoptetId(dto.getShoptetId());
        order.setOrderCode(dto.getOrderCode());
        */
        // ... další mapování polí (datum, email atd.)
        ShoptetOrder order = ShoptetOrder.builder().shoptetId(dto.getShoptetId()).orderCode(dto.getOrderCode()).date(dto.getDateCreated()).
                totalPrice(dto.getTotalPrice()).isPaid(dto.getPaidStatus()).customerEmail(dto.getCustomer().getEmail()).items(dto.getItems()).build();

        shoptetOrderRepository.save(order);
        log.info("Uložena nová servisní objednávka: " + dto.getOrderCode());
    }
}