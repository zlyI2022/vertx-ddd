package com.packagename.api;

import com.packagename.application.BlackboardService;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestVerticle extends AbstractVerticle {
    private final BlackboardService blackboardService;

    public RestVerticle(BlackboardService blackboardService) {
        this.blackboardService = blackboardService;
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);
        router.get("/health").handler(ctx -> ctx.response().end("ok"));
        router.get("/api/blackboard/:deptId").handler(this::handleGetCurrent);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080, result -> {
                if (result.succeeded()) {
                    log.info("HTTP 服务启动成功");
                } else {
                    log.error("HTTP 服务启动失败", result.cause());
                }
            });
    }

    private void handleGetCurrent(RoutingContext ctx) {
        String deptId = ctx.pathParam("deptId");
        blackboardService.getCurrentMarkdown(deptId)
            .subscribe(
                markdown -> ctx.response().putHeader("Content-Type", "text/plain").end(markdown),
                err -> {
                    log.error("获取看板失败", err);
                    ctx.response().setStatusCode(500).end("{\"error\":\"INTERNAL_ERROR\"}");
                }
            );
    }
}
