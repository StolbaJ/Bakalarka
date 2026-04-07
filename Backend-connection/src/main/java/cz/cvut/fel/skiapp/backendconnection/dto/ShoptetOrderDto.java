package cz.cvut.fel.skiapp.backendconnection.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ShoptetOrderDto {

    @JacksonXmlProperty(localName = "shoptetId")
    private String shoptetId;

    @JacksonXmlProperty(localName = "orderCode")
    private String orderCode;

    @JacksonXmlProperty(localName = "date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    @JacksonXmlProperty(localName = "totalPrice")
    private String totalPrice;

    @JacksonXmlProperty(localName = "isPaid")
    private String isPaid;

    @JacksonXmlProperty(localName = "customerEmail")
    private String customerEmail;

    @JacksonXmlProperty(localName = "customerPhone")
    private String customerPhone;

    @JacksonXmlProperty(localName = "customerFullName")
    private String customerFullName;
    @JacksonXmlProperty(localName = "customerAdress")
    private String customerAdress;

    @JacksonXmlElementWrapper(localName = "items")
    @JacksonXmlProperty(localName = "item")
    private List<ShoptetItemDto> items;
}
