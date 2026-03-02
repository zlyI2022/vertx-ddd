package com.packagename.blackboard.infrastructure.acl;

import com.packagename.blackboard.domain.facade.WorkspaceFacade;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import java.nio.file.Path;

public class LocalWorkspaceFacade implements WorkspaceFacade {
    private final Vertx vertx;
    private final Path workspaceDir;

    public LocalWorkspaceFacade(Vertx vertx, Path workspaceDir) {
        this.vertx = vertx;
        this.workspaceDir = workspaceDir;
    }

    @Override
    public Single<String> readBlackboard(String slug) {
        String path = workspaceDir.resolve(slug).resolve("blackboard.md").toString();
        return vertx.fileSystem().rxReadFile(path).map(buffer -> buffer.toString());
    }

    @Override
    public Completable writeBlackboard(String slug, String markdown) {
        String path = workspaceDir.resolve(slug).resolve("blackboard.md").toString();
        return vertx.fileSystem().rxWriteFile(path, io.vertx.rxjava3.core.buffer.Buffer.buffer(markdown)).ignoreElement();
    }
}
