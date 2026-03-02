package com.yepian;

import com.yepian.api.RestVerticle;
import com.yepian.application.BlackboardService;
import com.yepian.blackboard.domain.repository.BlackboardRepository;
import com.yepian.blackboard.infrastructure.persistence.PgBlackboardRepository;
import io.vertx.rxjava3.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

/**
 * 服务启动入口。
 *
 * <p>启动步骤：
 * 1. 检测环境 I/O 能力并创建 Vertx（io_uring -> epoll -> nio）。
 * 2. 执行 Flyway 数据库迁移。
 * 3. 构建仓储、应用服务、HTTP 适配器并完成部署。
 */
@Slf4j
public final class MainLauncher {
    private MainLauncher() {
    }

    public static void main(String[] args) {
        Vertx vertx = IoTransportDetector.createVertxByEnvironment();

        vertx.rxExecuteBlocking(promise -> {
            log.info("数据库迁移开始");
            Flyway.configure()
                .dataSource(System.getenv("DATABASE_URL"), null, null)
                .load()
                .migrate();
            log.info("数据库迁移完成");
            promise.complete();
        }).subscribe(
            ok -> {
                BlackboardRepository repository = new PgBlackboardRepository(vertx);
                BlackboardService blackboardService = new BlackboardService(repository);
                RestVerticle restVerticle = new RestVerticle(blackboardService);
                vertx.deployVerticle(restVerticle);
                log.info("服务启动完成");
            },
            err -> log.error("服务启动失败", err)
        );
    }
}
