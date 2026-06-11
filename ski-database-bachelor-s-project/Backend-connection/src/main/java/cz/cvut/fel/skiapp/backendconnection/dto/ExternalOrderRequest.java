package cz.cvut.fel.skiapp.backendconnection.dto;

import lombok.Data;

@Data
public class ExternalOrderRequest {
    private Long customerId;
    private long[] skiIds;
    private String[] targetStruktura; // Např. ["1:0.7"]
    private Long pohodaId;
}
