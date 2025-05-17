package com.github.shen.canary.server.dao;

import com.github.shen.canary.server.entity.TaskLog;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;


@Repository
public interface TaskLogMapper extends Mapper<TaskLog> {

}
