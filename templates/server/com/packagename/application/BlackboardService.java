package com.packagename.application;

import com.packagename.blackboard.domain.repository.BlackboardRepository;
import io.reactivex.rxjava3.core.Single;

public class BlackboardService {
    private final BlackboardRepository repository;

    public BlackboardService(BlackboardRepository repository) {
        this.repository = repository;
    }

    public Single<String> getCurrentMarkdown(String departmentId) {
        return repository.findCurrentByDepartmentId(departmentId)
            .map(blackboard -> blackboard.getMarkdown());
    }
}
