package com.yepian.snippets;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * MainLauncher 启动期模板示例：
 * 1. 按 io_uring -> epoll -> nio 选择 I/O 传输能力。
 * 2. 统一初始化 EventBus 默认编解码器。
 * 3. 为本地高频消息提供 localOnly 配置。
 */
@Slf4j
public final class MainLauncherCodecRegisterExample {
    /**
     * 启动期幂等保护开关。
     * 防止重复执行初始化逻辑造成 default codec 重复注册。
     */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private MainLauncherCodecRegisterExample() {
    }

    /**
     * 创建 Vertx 实例。
     *
     * <p>内部会先检测运行环境可用 I/O，并按优先级选择：
     * io_uring -> epoll -> nio。
     */
    public static Vertx createVertx() {
        VertxOptions options = IoTransportDetector.createVertxOptionsByEnvironment();
        return Vertx.vertx(options);
    }

    /**
     * 在 MainLauncher 启动阶段调用一次。
     *
     * @param vertx Vert.x 实例，不能为空
     */
    public static void registerEventBusCodecs(Vertx vertx) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            log.warn("MainLauncher 编解码器已初始化，跳过重复注册");
            return;
        }

        EventBus eventBus = Objects.requireNonNull(vertx, "vertx 不能为空").eventBus();

        EventBusCodecRegistrar.registerAll(
            eventBus,
            EventBusCodecRegistrar.binding(UserCreatedCmd.class, "codec.user.created.cmd"),
            EventBusCodecRegistrar.binding(UserCreatedResult.class, "codec.user.created.result")
        );

        log.info("MainLauncher 编解码器初始化完成");
    }

    /**
     * 高频本地请求统一使用该配置，确保走本地引用传递。
     *
     * <p>说明：
     * - localOnly=true 可确保消息不走网络编码路径。
     * - 结合 LocalRefMessageCodec 的 transform 引用返回，可减少对象分配。
     */
    public static DeliveryOptions localOnlyOptions() {
        return new DeliveryOptions().setLocalOnly(true);
    }

    /**
     * 示例消息对象：发送后按只读对象处理。
     */
    public record UserCreatedCmd(String userId, String nickname) {
    }

    /**
     * 示例响应对象：发送后按只读对象处理。
     */
    public record UserCreatedResult(String userId, boolean success) {
    }
}
