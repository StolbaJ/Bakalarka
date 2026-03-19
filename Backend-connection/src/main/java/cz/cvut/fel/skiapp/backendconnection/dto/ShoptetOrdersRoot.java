package cz.cvut.fel.skiapp.backendconnection.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JacksonXmlRootElement(localName = "orders")
public class ShoptetOrdersRoot {

    // Getter a Setter
    @JacksonXmlProperty(localName = "order")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<OrderDto> orders;

}
