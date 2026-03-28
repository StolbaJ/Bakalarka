package cz.cvut.fel.skiapp.backendconnection.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Oprava importu
import cz.cvut.fel.skiapp.backendconnection.dto.OrderDto;
import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetOrderDto;
import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetOrdersRoot;
import cz.cvut.fel.skiapp.backendconnection.mapper.ShoptetMapper;
import cz.cvut.fel.skiapp.backendconnection.model.ServiceOrder;
import cz.cvut.fel.skiapp.backendconnection.repository.ServiceOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShoptetImportService {

    private final ServiceOrderRepository orderRepository;
    private final XmlMapper xmlMapper;
    private final ShoptetMapper shoptetMapper;

    private static final List<String> SERVICE_SKU_CODES = List.of("RC_018", "RC_081", "RC_084");

    @Scheduled(cron = "0 0/30 * * * *")
    public void importOrdersFromShoptet() {
        String url = "https://vas-eshop.cz/export/orders.xml";

        try {
            ShoptetOrdersRoot root = xmlMapper.readValue(new URL(url), ShoptetOrdersRoot.class);
            if (root.getOrders() == null) return;

            for (ShoptetOrderDto dto : root.getOrders()) {
                if (containsServiceItem(dto) && !orderRepository.existsByShoptetId(dto.getShoptetId())) {

                    // POUŽITÍ MAPPERU
                    ServiceOrder entity = shoptetMapper.toEntity(dto);
                    orderRepository.save(entity);

                    log.info("Importována zakázka {} ze Shoptetu", dto.getOrderCode());
                }
            }
        } catch (IOException e) {
            log.error("Chyba importu: {}", e.getMessage());
        }
    }

    private boolean containsServiceItem(ShoptetOrderDto dto) {
        return dto.getItems().stream()
                .anyMatch(i -> SERVICE_SKU_CODES.contains(i.getProductCode()));
    }
}