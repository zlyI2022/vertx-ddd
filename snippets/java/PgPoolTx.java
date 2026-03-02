package com.packagename.snippets;

import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.SqlClient;
import java.util.Objects;
import java.util.function.Function;

/**
 * PgPool 事务执行模板。
 *
 * <p>设计目的：
 * 1. 统一事务边界写法，避免各业务服务重复拼装 `rxWithTransaction`。
 * 2. 强制调用方以响应式方式组织事务内逻辑，避免引入阻塞调用。
 */
public final class PgPoolTx {
    private PgPoolTx() {
    }

    /**
     * 在单个事务内执行响应式工作流。
     *
     * <p>注意事项：
     * 1. `work` 中仅允许使用响应式 I/O（数据库、网络、文件等）。
     * 2. 禁止在事务内调用阻塞 API（如 Thread.sleep、同步文件读写）。
     * 3. 事务成功由返回的 `Single` 正常结束决定；异常将触发回滚。
     *
     * @param pool 事务连接池，不能为空
     * @param work 事务工作函数，输入事务内 `SqlClient`，返回业务结果
     * @param <T>  业务结果类型
     * @return 事务结果 Single
     */
    public static <T> Single<T> withTx(PgPool pool, Function<SqlClient, Single<T>> work) {
        Objects.requireNonNull(pool, "pool 不能为空");
        Objects.requireNonNull(work, "work 不能为空");
        return pool.rxWithTransaction(work::apply);
    }
}
