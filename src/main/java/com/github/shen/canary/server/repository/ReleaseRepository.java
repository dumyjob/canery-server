package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.domain.Release;

public interface ReleaseRepository {


    Release get(Long releaseId);
}
