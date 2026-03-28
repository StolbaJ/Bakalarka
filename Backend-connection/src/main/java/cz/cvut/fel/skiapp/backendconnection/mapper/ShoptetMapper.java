package cz.cvut.fel.skiapp.backendconnection.mapper;

import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetItemDto;
import cz.cvut.fel.skiapp.backendconnection.dto.ShoptetOrderDto;
import cz.cvut.fel.skiapp.backendconnection.model.OrderItem;
import cz.cvut.fel.skiapp.backendconnection.model.OrderSource;
import cz.cvut.fel.skiapp.backendconnection.model.OrderStatus;
import cz.cvut.fel.skiapp.backendconnection.model.ServiceOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
