package com.github.shen.canary.server.repository.impl;

import com.github.shen.canary.server.dao.ReleaseOrderMapper;
import com.github.shen.canary.server.dao.ReleaseProjectMapper;
import com.github.shen.canary.server.domain.Release;
import com.github.shen.canary.server.entity.ReleaseOrderBean;
import com.github.shen.canary.server.entity.ReleaseProjectBean;
import com.github.shen.canary.server.exceptions.DatabaseException;
import com.github.shen.canary.server.repository.ReleaseRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.weekend.Weekend;

import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
public class ReleaseRepositoryImpl implements ReleaseRepository {

    private final ReleaseOrderMapper releaseOrderMapper;
    private final ReleaseProjectMapper releaseProjectMapper;

    @Override
    public Release get(Long releaseId) {
        // 可能需要获取当前发布的时候的git-repos/branch的commit-id
        final ReleaseOrderBean releaseOrderBean = releaseOrderMapper.selectByPrimaryKey(releaseId);
        if (Objects.isNull(releaseOrderBean)) {
            throw new DatabaseException("未找到数据,releaseId:" + releaseId);
        }


        Weekend<ReleaseProjectBean> releaseProjectExample = Weekend.of(ReleaseProjectBean.class);
        releaseProjectExample.weekendCriteria()
                .andEqualTo(ReleaseProjectBean::getReleaseId, releaseId);

        final List<ReleaseProjectBean> releaseProjects = releaseProjectMapper.selectByExample(releaseProjectExample);
        if (CollectionUtils.isEmpty(releaseProjects)) {
            throw new DatabaseException("发布单项目为空");
        }

        return Release.valueOf(releaseOrderBean, releaseProjects);
    }
}
