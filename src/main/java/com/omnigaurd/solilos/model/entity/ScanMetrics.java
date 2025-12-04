package com.omnigaurd.solilos.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "scan_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @Column(name = "metric_name", length = 100)
    private String metricName;

    @Column(name = "metric_value", precision = 10, scale = 2)
    private BigDecimal metricValue;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
