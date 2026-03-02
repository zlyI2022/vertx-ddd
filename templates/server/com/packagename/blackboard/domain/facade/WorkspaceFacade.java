package com.packagename.blackboard.domain.facade;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public interface WorkspaceFacade {
    Single<String> readBlackboard(String slug);
    Completable writeBlackboard(String slug, String markdown);
}
