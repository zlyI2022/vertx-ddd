package com.packagename.snippets;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 本地 EventBus 引用传递编解码器模板。
 *
 * <p>适用场景：
 * 1. 仅在同 JVM 进程内通信（`DeliveryOptions#setLocalOnly(true)`）。
 * 2. 追求高频消息路径性能，减少对象复制与序列化开销。
 *
 * <p>重要约束：
 * 1. `transform` 直接返回原对象引用，发送后对象必须按只读对象对待。
 * 2. 本类不支持跨节点传输；若存在集群通信需求，必须单独实现 wire codec。
 */
public final class LocalRefMessageCodec<T> implements MessageCodec<T, T> {
    /**
     * 编解码器名称格式：
     * - 长度 3~128
     * - 允许字符：字母、数字、点、下划线、短横线
     */
    private static final Pattern CODEC_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,128}$");

    private final String codecName;
    private final Class<T> targetType;

    /**
     * 构造函数。
     *
     * @param targetType 目标消息类型，不能为空且不能是基本类型
     * @param codecName  编解码器名称，需符合命名规范
     */
    public LocalRefMessageCodec(Class<T> targetType, String codecName) {
        this.targetType = validateType(targetType);
        this.codecName = normalizeCodecName(codecName);
    }

    /**
     * 本地引用模式下不支持跨节点编码。
     */
    @Override
    public void encodeToWire(Buffer buffer, T body) {
        throw new UnsupportedOperationException("本地引用编解码器不支持跨节点编码");
    }

    /**
     * 本地引用模式下不支持跨节点解码。
     */
    @Override
    public T decodeFromWire(int pos, Buffer buffer) {
        throw new UnsupportedOperationException("本地引用编解码器不支持跨节点解码");
    }

    /**
     * 本地消息转换：返回同一对象引用，避免 clone/序列化导致的额外分配。
     */
    @Override
    public T transform(T body) {
        return body;
    }

    @Override
    public String name() {
        return codecName;
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }

    /**
     * @return 该编解码器绑定的消息类型
     */
    public Class<T> targetType() {
        return targetType;
    }

    /**
     * 便捷注册默认编解码器。
     *
     * <p>建议在 MainLauncher 启动阶段集中注册，避免重复注册。
     */
    public static <T> void registerDefault(EventBus eventBus, Class<T> type, String codecName) {
        Objects.requireNonNull(eventBus, "eventBus 不能为空");
        LocalRefMessageCodec<T> codec = new LocalRefMessageCodec<>(type, codecName);
        try {
            eventBus.registerDefaultCodec(type, codec);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                "默认编解码器注册失败，可能重复注册。请在 MainLauncher 启动阶段统一注册。类型=" + type.getName(),
                e
            );
        }
    }

    private static <T> Class<T> validateType(Class<T> type) {
        Class<T> nonNullType = Objects.requireNonNull(type, "消息类型不能为空");
        if (nonNullType.isPrimitive()) {
            throw new IllegalArgumentException("消息类型不能是基本类型: " + nonNullType.getName());
        }
        return nonNullType;
    }

    private static String normalizeCodecName(String codecName) {
        String nonNullName = Objects.requireNonNull(codecName, "编解码器名称不能为空").trim();
        if (!CODEC_NAME_PATTERN.matcher(nonNullName).matches()) {
            throw new IllegalArgumentException("编解码器名称不合法: " + codecName + "，仅允许字母/数字/._-，长度 3~128");
        }
        return nonNullName;
    }
}
