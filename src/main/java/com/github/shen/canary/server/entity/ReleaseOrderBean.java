package com.github.shen.canary.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;



@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "release_orders")
public class ReleaseOrderBean extends DataBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "JDBC")
    private Long id;

    @Column(name = "release_name")
    private String releaseName;
    @Column(name = "release_type")
    private String releaseType;

    private String env;

    @Column(name = "gray_version")
    private String grayVersion;

    private String status;

    @Column(name = "traffic_rule")
    private String trafficRule;

}
