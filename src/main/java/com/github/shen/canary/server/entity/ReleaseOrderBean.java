package com.github.shen.canary.server.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "release_order")
public class ReleaseOrderBean extends DataBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_name")
    private String releaseName;
    @Column(name = "release_type")
    private String releaseType;

    private String env;

    private String status;

    @Column(name = "traffic_rule")
    private String trafficRule;

}
