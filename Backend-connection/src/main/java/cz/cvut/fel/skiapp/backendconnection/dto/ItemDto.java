package cz.cvut.fel.skiapp.backendconnection.dto;

import lombok.Data;

@Data
public class ItemDto {
    private String name;
    private String productCode;
    private String quantity;
    private String remark;
}
