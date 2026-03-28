package cz.cvut.fel.skiapp.backendconnection.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShoptetItemDto {
    private String name;
    private String amount;
    private String productCode;
    private String remark;
}