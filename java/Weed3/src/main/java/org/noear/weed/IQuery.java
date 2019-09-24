package org.noear.weed;

import org.noear.weed.cache.CacheUsing;
import org.noear.weed.cache.ICacheService;
import org.noear.weed.ext.Act2;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by noear on 14/11/12.
 */
public interface IQuery {
     long getCount() throws SQLException;
     Object getValue() throws SQLException;
     <T> T getValue(T def) throws SQLException;

     Variate getVariate() throws SQLException;
     Variate getVariate(Act2<CacheUsing,Variate> cacheCondition) throws SQLException;

     <T extends IBinder> T getItem(T model) throws SQLException;
     <T extends IBinder> T getItem(T model, Act2<CacheUsing, T> cacheCondition) throws SQLException;


     <T extends IBinder> List<T> getList(T model) throws SQLException;
     <T extends IBinder> List<T> getList(T model, Act2<CacheUsing, List<T>> cacheCondition) throws SQLException;

     <T> T getItem(Class<T> cls) throws SQLException,ReflectiveOperationException;

     <T> List<T> getList(Class<T> cls) throws SQLException,ReflectiveOperationException;

     DataList getDataList() throws SQLException;
     DataList getDataList(Act2<CacheUsing, DataList> cacheCondition) throws SQLException;
     DataItem getDataItem() throws SQLException;
     DataItem getDataItem(Act2<CacheUsing, DataItem> cacheCondition) throws SQLException;

     List<Map<String,Object>> getMapList() throws SQLException;
     Map<String,Object> getMap() throws SQLException;

     <T> List<T> getArray(String column) throws SQLException;

     IQuery caching(ICacheService service);
     IQuery usingCache(boolean isCache);
     IQuery usingCache(int seconds);
     IQuery cacheTag(String tag);
}
