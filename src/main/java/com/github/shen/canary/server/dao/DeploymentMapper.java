package com.github.shen.canary.server.dao;

import com.github.shen.canary.server.entity.Deployment;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;


@Repository
public interface DeploymentMapper extends Mapper<Deployment> {

}
