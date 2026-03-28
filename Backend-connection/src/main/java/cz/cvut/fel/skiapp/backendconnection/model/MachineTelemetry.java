package cz.cvut.fel.skiapp.backendconnection.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "machine_telemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineTelemetry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String machineId; // ID stroje, např. "SkiServiceMachine1"
    private String status; // Stav stroje, např. "Idle", "Running", "Error"
    private String currentOperation; // Aktuální operace, např. "Grinding", "Polishing"
    private String errorCode; // Kód chyby, pokud je stav "Error"
}

