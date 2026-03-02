package com.packagename.blackboard.domain.repository;

import com.packagename.blackboard.domain.model.Blackboard;
import io.reactivex.rxjava3.core.Single;

public interface BlackboardRepository {
    Single<Blackboard> findCurrentByDepartmentId(String departmentId);
}
