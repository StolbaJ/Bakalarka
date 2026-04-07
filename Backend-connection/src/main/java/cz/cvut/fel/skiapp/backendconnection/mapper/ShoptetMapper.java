package cz.cvut.fel.skiapp.backendconnection.mapper;

import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetItemDto;
import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetOrderDto;
import cz.cvut.fel.skiapp.backendconnection.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ShoptetMapper {

    public ServiceOrder toEntity(ShoptetOrderDto dto) {
        if (dto == null) return null;

        ServiceOrder order = new ServiceOrder();

        // Základní metadata
        order.setShoptetId(dto.getShoptetId());
        order.setOrderCode(dto.getOrderCode());
        order.setCreatedAt(dto.getDate());
        order.setSource(OrderSource.SHOPTET);
        order.setStatus(OrderStatus.QUEUED); // Nový import je vždy ve frontě

        // Zákaznická data
        order.setCustomerEmail(dto.getCustomerEmail());
        order.setCustomerPhone(dto.getCustomerPhone());

        // Převod ceny: "219,00" -> 219.00
        if (dto.getTotalPrice() != null) {
            String cleanPrice = dto.getTotalPrice().replace(",", ".");
            order.setTotalPrice(new BigDecimal(cleanPrice));
        }

        order.setIsPaid(dto.getIsPaid());

        // Mapování položek
        if (dto.getItems() != null) {
            List<OrderItem> entityItems = dto.getItems().stream()
                    .map(itemDto -> mapItem(itemDto, order))
                    .toList();
            order.setItems(entityItems);
        }

        return order;
    }

    public ServiceOrder toEntity(ShoptetOrderDto dto, Customer customer) {
        if (dto == null) return null;

        ServiceOrder order = ServiceOrder.builder()
                .shoptetId(dto.getShoptetId())
                .orderCode(dto.getOrderCode())
                .createdAt(dto.getDate())
                .source(OrderSource.SHOPTET)
                .status(OrderStatus.QUEUED)
                .customer(customer) // Tady propojíme objednávku s tím, co servisa našla
                .customerEmail(dto.getCustomerEmail())
                .customerPhone(dto.getCustomerPhone())
                .isPaid(dto.getIsPaid() != null ? dto.getIsPaid() : "0")
                .items(new ArrayList<>())
                .build();

        // Převod ceny (řešíme čárku v XML)
        if (dto.getTotalPrice() != null && !dto.getTotalPrice().isEmpty()) {
            order.setTotalPrice(new BigDecimal(dto.getTotalPrice().replace(",", ".")));
        }

        // Mapování položek (Items) - Tímto se uloží do tabulky order_items
        if (dto.getItems() != null) {
            for (var itemDto : dto.getItems()) {
                OrderItem item = new OrderItem();
                item.setOrder(order); // Zpětná vazba pro JPA
                //item.setProductName(itemDto.getName());
                item.setProductCode(itemDto.getProductCode());
                item.setAmount(Integer.parseInt(itemDto.getAmount() != null ? itemDto.getAmount() : "1"));
                item.setRemark(itemDto.getRemark());

                order.getItems().add(item);
            }
        }

        return order;
    }

    private OrderItem mapItem(ShoptetItemDto itemDto, ServiceOrder order) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setTaskName(itemDto.getName());
        item.setTaskInstruction(itemDto.getProductCode()); // Můžeme použít jako kód úkonu
        //item.setStatus(ItemStatus.WAITING);
        // Ski a další vazby se dořeší později v servise
        return item;
    }
}
