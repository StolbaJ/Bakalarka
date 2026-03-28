package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


public enum OrderStatus {
    QUEUED, SYNCED, IN_PROGRESS, COMPLETED, FAILED;
}