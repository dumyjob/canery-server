package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.domain.Release;
import com.github.shen.canary.server.web.request.ReleaseSearch;

import java.util.List;

public interface ReleaseRepository {


    Release get(Long releaseId);

    List<Release> get(ReleaseSearch request);

    void remove(Long releaseId);

    Release save(Release release);

    Release update(Release release);
}
