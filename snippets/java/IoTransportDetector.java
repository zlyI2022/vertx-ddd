package com.yepian.snippets;

import io.vertx.core.VertxOptions;
import java.lang.reflect.Method;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

/**
 * Vert.x 运行时 I/O 传输能力检测模板。
 *
 * <p>检测优先级固定为：
 * 1. io_uring
 * 2. epoll
 * 3. nio（兜底）
 *
 * <p>设计原则：
 * 1. 通过反射检测 Netty Native Transport，避免对特定 native 依赖的编译期强绑定。
 * 2. 任一检测异常都不应中断启动流程，最终必须可降级到 NIO。
 */
@Slf4j
public final class IoTransportDetector {
    private static final String IO_URING_CLASS = "io.netty.channel.uring.IOUring";
    private static final String EPOLL_CLASS = "io.netty.channel.epoll.Epoll";

    private IoTransportDetector() {
    }

    /**
     * I/O 传输模式。
     */
    public enum IoMode {
        IO_URING,
        EPOLL,
        NIO
    }

    /**
     * 按约定优先级检测可用 I/O 模式。
     *
     * @return 选中的 I/O 模式
     */
    public static IoMode detectPreferredMode() {
        if (!isLinux()) {
            log.info("当前操作系统非 Linux，直接使用 NIO。os={}", System.getProperty("os.name"));
            return IoMode.NIO;
        }

        if (isTransportAvailable(IO_URING_CLASS)) {
            log.info("检测到 io_uring 可用，优先使用 io_uring");
            return IoMode.IO_URING;
        }

        if (isTransportAvailable(EPOLL_CLASS)) {
            log.info("未检测到 io_uring，检测到 epoll 可用，使用 epoll");
            return IoMode.EPOLL;
        }

        log.warn("未检测到 io_uring/epoll，可用模式降级为 NIO");
        return IoMode.NIO;
    }

    /**
     * 根据检测结果构建 Vert.x 配置。
     *
     * <p>说明：
     * - 当模式为 NIO 时，关闭 preferNativeTransport。
     * - 当模式为 io_uring/epoll 时，开启 preferNativeTransport。
     */
    public static VertxOptions createVertxOptionsByEnvironment() {
        IoMode mode = detectPreferredMode();
        VertxOptions options = new VertxOptions();
        options.setPreferNativeTransport(mode != IoMode.NIO);
        log.info("I/O 模式已确定: mode={}, preferNativeTransport={}", mode, mode != IoMode.NIO);
        return options;
    }

    private static boolean isLinux() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("linux");
    }

    /**
     * 反射检测指定 Netty Transport 是否可用。
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
            log.warn("Transport 检测失败，按不可用处理: class={}, error={}", transportClassName, e.getMessage());
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
