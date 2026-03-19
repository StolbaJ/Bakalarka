package cz.cvut.fel.skiapp.backendconnection.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import cz.cvut.fel.skiapp.backendconnection.model.Customer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
public class OrderDto {
    private String shoptetId;
    private String orderCode;
    private LocalDateTime dateCreated;
    private String totalPrice;
    private String paidStatus;
    private Customer customer;

    @JacksonXmlElementWrapper(localName = "orderItems")
    @JacksonXmlProperty(localName = "item")
    private List<ItemDto> items;
}

