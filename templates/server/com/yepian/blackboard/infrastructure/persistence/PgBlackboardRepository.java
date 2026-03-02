package com.yepian.blackboard.infrastructure.persistence;

import com.yepian.blackboard.domain.model.Blackboard;
import com.yepian.blackboard.domain.repository.BlackboardRepository;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;

public class PgBlackboardRepository implements BlackboardRepository {
    private final PgPool pool;

    public PgBlackboardRepository(Vertx vertx) {
        this.pool = PgPool.pool(vertx);
    }

    @Override
    public Single<Blackboard> findCurrentByDepartmentId(String departmentId) {
        String sql = "select id, department_id, markdown from blackboard_documents where department_id = $1 limit 1";
        return pool.rxPreparedQuery(sql)
            .execute(Tuple.of(departmentId))
            .map(rows -> {
                Row row = rows.iterator().hasNext() ? rows.iterator().next() : null;
                if (row == null) {
                    throw new IllegalStateException("NOT_FOUND");
                }
                return new Blackboard(
                    row.getUUID("id").toString(),
                    row.getUUID("department_id").toString(),
                    row.getString("markdown")
                );
            });
    }
}
