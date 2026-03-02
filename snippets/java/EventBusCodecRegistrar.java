package com.yepian.snippets;

import io.vertx.core.eventbus.EventBus;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * EventBus 编解码器批量注册器（含启动期校验）。
 *
 * <p>设计目的：
 * 1. 统一在 MainLauncher 启动阶段完成一次性注册。
 * 2. 在注册前做完整参数校验，尽早失败并输出可定位信息。
 */
@Slf4j
public final class EventBusCodecRegistrar {
    private EventBusCodecRegistrar() {
    }

    /**
     * 单条绑定定义：消息类型 + 编解码器名称。
     */
    public record CodecBinding<T>(Class<T> type, String codecName) {
    }

    /**
     * 创建绑定定义。
     */
    public static <T> CodecBinding<T> binding(Class<T> type, String codecName) {
        return new CodecBinding<>(
            Objects.requireNonNull(type, "消息类型不能为空"),
            Objects.requireNonNull(codecName, "编解码器名称不能为空")
        );
    }

    /**
     * 批量注册默认编解码器。
     *
     * <p>内置校验：
     * 1. `bindings` 不能为空且至少 1 条。
     * 2. 绑定项不能为空，消息类型不能为空。
     * 3. 编解码器名称不能为空字符串。
     * 4. 不允许重复消息类型绑定。
     * 5. 不允许重复编解码器名称。
     */
    public static void registerAll(EventBus eventBus, CodecBinding<?>... bindings) {
        Objects.requireNonNull(eventBus, "eventBus 不能为空");
        Objects.requireNonNull(bindings, "bindings 不能为空");
        if (bindings.length == 0) {
            throw new IllegalArgumentException("至少需要提供一个编解码器绑定");
        }

        Set<Class<?>> typeSet = new HashSet<>();
        Set<String> codecNameSet = new HashSet<>();

        Arrays.stream(bindings).forEach(binding -> {
            validateBinding(binding);
            if (!typeSet.add(binding.type())) {
                throw new IllegalArgumentException("存在重复的消息类型绑定: " + binding.type().getName());
            }
            if (!codecNameSet.add(binding.codecName().trim())) {
                throw new IllegalArgumentException("存在重复的编解码器名称: " + binding.codecName());
            }
        });

        Arrays.stream(bindings).forEach(binding -> registerOne(eventBus, binding));
    }

    /**
     * 注册单条绑定。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerOne(EventBus eventBus, CodecBinding<?> binding) {
        LocalRefMessageCodec codec = new LocalRefMessageCodec(binding.type(), binding.codecName());
        try {
            eventBus.registerDefaultCodec((Class) binding.type(), codec);
            log.info("EventBus 编解码器注册成功: type={}, codec={}", binding.type().getName(), codec.name());
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                "EventBus 编解码器注册失败，可能重复注册。请确保仅在 MainLauncher 启动阶段调用一次。type="
                    + binding.type().getName() + ", codec=" + binding.codecName(),
                e
            );
        }
    }

    /**
     * 绑定项参数校验。
     */
    private static void validateBinding(CodecBinding<?> binding) {
        Objects.requireNonNull(binding, "CodecBinding 不能为空");
        Objects.requireNonNull(binding.type(), "消息类型不能为空");

        String codecName = Objects.requireNonNull(binding.codecName(), "编解码器名称不能为空").trim();
        if (codecName.isEmpty()) {
            throw new IllegalArgumentException("编解码器名称不能为空字符串");
        }
    }
}
