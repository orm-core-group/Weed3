package org.noear.weed;

import org.noear.weed.ext.Act1;

import java.util.List;
import java.util.Map;

/**
 * Created by noear on 19-12-11.
 */
public interface BaseMapper<T> {
    Long insert(T entity, boolean excludeNull);
    void insertList(List<T> list);

    Integer deleteById(Object id);
    Integer deleteByIds(Iterable<Object> idList);
    Integer deleteByMap(Map<String, Object> columnMap);
    Integer delete(Act1<WhereQ> condition);

    Integer updateById(T entity, boolean excludeNull);
    Integer update(T entity, boolean excludeNull, Act1<WhereQ> condition);

    Long upsert(T entity, boolean excludeNull);
    Long upsertBy(T entity, boolean excludeNull, String conditionFields);

    boolean existsById(Object id);
    boolean exists(Act1<WhereQ> condition);

    T selectById(Object id);
    List<T> selectByIds(Iterable<Object> idList);
    List<T> selectByMap(Map<String, Object> columnMap);

    T selectItem(T entity);
    T selectItem(Act1<WhereQ> condition);
    Map<String, Object> selectMap(Act1<WhereQ> condition);

    Object selectValue(String column, Act1<WhereQ> condition);

    Long selectCount(Act1<WhereQ> condition);

    List<T> selectList(Act1<WhereQ> condition);
    List<Map<String, Object>> selectMapList(Act1<WhereQ> condition);
    List<Object> selectArray(String column, Act1<WhereQ> condition);

    /**
     * @param start 从0开始
     * */
    List<T> selectPage(int start, int rows, Act1<WhereQ> condition);
    List<Map<String, Object>> selectMapPage(int start, int rows, Act1<WhereQ> condition);

    List<T> selectTop(int top, Act1<WhereQ> condition);
    List<Map<String, Object>> selectMapTop(int top, Act1<WhereQ> condition);
}
