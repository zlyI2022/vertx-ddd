package com.yepian;

import io.vertx.core.VertxOptions;
import io.vertx.rxjava3.core.Vertx;
import java.lang.reflect.Method;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

/**
 * Vert.x 运行时 I/O 传输能力检测器。
 *
 * <p>检测优先级固定为：io_uring -> epoll -> nio。
 * 检测过程中出现异常时不得中断启动，必须降级到 NIO 并输出中文日志。
 */
@Slf4j
public final class IoTransportDetector {
    private static final String IO_URING_CLASS = "io.netty.channel.uring.IOUring";
    private static final String EPOLL_CLASS = "io.netty.channel.epoll.Epoll";

    private IoTransportDetector() {
    }

    /**
     * I/O 模式枚举。
     */
    public enum IoMode {
        IO_URING,
        EPOLL,
        NIO
    }

    /**
     * 根据当前环境创建 Vertx 实例。
     *
     * <p>内部流程：
     * 1. 检测最优 I/O 模式。
     * 2. 生成对应 VertxOptions。
     * 3. 返回 RxJava3 Vertx 包装实例。
     */
    public static Vertx createVertxByEnvironment() {
        VertxOptions options = createVertxOptionsByEnvironment();
        return Vertx.vertx(options);
    }

    /**
     * 按 io_uring -> epoll -> nio 顺序检测可用 I/O 模式。
     */
    public static IoMode detectPreferredMode() {
        if (!isLinux()) {
            log.info("当前系统非 Linux，使用 NIO。os={}", System.getProperty("os.name"));
            return IoMode.NIO;
        }

        if (isTransportAvailable(IO_URING_CLASS)) {
            log.info("检测到 io_uring 可用，使用 io_uring");
            return IoMode.IO_URING;
        }

        if (isTransportAvailable(EPOLL_CLASS)) {
            log.info("未检测到 io_uring，检测到 epoll 可用，使用 epoll");
            return IoMode.EPOLL;
        }

        log.warn("io_uring 与 epoll 均不可用，降级为 NIO");
        return IoMode.NIO;
    }

    /**
     * 根据 I/O 模式构建 VertxOptions。
     *
     * <p>说明：
     * - IO_URING / EPOLL: 开启 preferNativeTransport。
     * - NIO: 关闭 preferNativeTransport。
     */
    public static VertxOptions createVertxOptionsByEnvironment() {
        IoMode mode = detectPreferredMode();
        VertxOptions options = new VertxOptions();
        options.setPreferNativeTransport(mode != IoMode.NIO);
        log.info("I/O 模式确定完成: mode={}, preferNativeTransport={}", mode, mode != IoMode.NIO);
        return options;
    }

    private static boolean isLinux() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("linux");
    }

    /**
     * 通过反射检查 Netty Native Transport 可用性。
     *
     * <p>使用反射的原因：
     * - 避免模板在未引入 native 依赖时出现编译期硬绑定问题。
     */
    private static boolean isTransportAvailable(String transportClassName) {
        try {
            Class<?> transportClass = Class.forName(transportClassName);
            Method isAvailableMethod = transportClass.getMethod("isAvailable");
            Object availableObj = isAvailableMethod.invoke(null);

            if (availableObj instanceof Boolean available) {
                if (!available) {
                    log.debug("Transport 不可用: class={}, reason={}", transportClassName, resolveUnavailabilityCause(transportClass));
                }
                return available;
            }

            log.warn("Transport 可用性返回类型异常，按不可用处理: class={}, valueType={}",
                transportClassName,
                availableObj == null ? "null" : availableObj.getClass().getName());
            return false;
        } catch (ClassNotFoundException e) {
            log.debug("未找到 Transport 类，按不可用处理: class={}", transportClassName);
            return false;
        } catch (ReflectiveOperationException e) {
            log.warn("Transport 检测异常，按不可用处理: class={}, error={}", transportClassName, e.getMessage());
            return false;
        }
    }

    private static String resolveUnavailabilityCause(Class<?> transportClass) {
        try {
            Method causeMethod = transportClass.getMethod("unavailabilityCause");
            Object causeObj = causeMethod.invoke(null);
            if (causeObj instanceof Throwable throwable) {
                return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
            }
        } catch (ReflectiveOperationException ignored) {
            // 忽略原因解析失败，不影响主流程。
        }
        return "无详细原因";
    }
}
