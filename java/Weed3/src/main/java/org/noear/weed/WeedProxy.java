package org.noear.weed;

import org.noear.weed.annotation.DbNamspace;
import org.noear.weed.utils.StringUtils;
import org.noear.weed.xml.XmlSqlBlock;
import org.noear.weed.xml.XmlSqlFactory;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WeedProxy {

    /** 创建接口调用代理客户端 */
    public static  <T> T get(Class<?> clz ) {
        return (T) Proxy.newProxyInstance(
                clz.getClassLoader(),
                new Class[]{clz},
                (proxy, method, args) -> proxy_call(proxy, method, args));
    }

    /** 执行调用代理 */
    private static Object proxy_call(Object proxy, Method method, Object[] vals) throws Throwable {
        //1.构建xml namme
        Class<?> clazz = method.getDeclaringClass();

        DbNamspace c_meta = clazz.getAnnotation(DbNamspace.class);
        String fun_name = method.getName();

        String xml_name = null;
        if(c_meta == null){
            xml_name = clazz.getPackage().getName()+"."+fun_name;
        }else {
            xml_name = c_meta.value() + "." + fun_name;
        }

        //2.构建参数
        Map<String, Object> _map = new HashMap<>();
        Parameter[] names = method.getParameters();
        for (int i = 0, len = names.length; i < len; i++) {
            if (vals[i] != null) {
                _map.put(names[i].getName(), vals[i]);
            }
        }

        XmlSqlBlock block = XmlSqlFactory.get(xml_name);
        if(block == null){
            throw new RuntimeException("Xmlsql does not exist:" +xml_name);
        }

        if(StringUtils.isEmpty(block._db)){
            throw new RuntimeException(xml_name +":missing :db configuration");
        }
        DbContext db = WeedConfig.libOfDb.get(block._db);
        if(db == null){
            throw new RuntimeException("WeedConfig.libOfDb does not exist:@"+block._db);
        }

        //3.生成命令
        DbProcedure sp = db.call("@"+xml_name).setMap(_map);

        //4.构建输出
        Class<?> rst_type = method.getReturnType();
        Type     rst_type2 = method.getGenericReturnType();

        if(block.isSelect()){
            if(block._return.indexOf(".")>0){
                //实体化处理
                if(Collection.class.isAssignableFrom(rst_type)){
                    //是实体集合
                    rst_type2  =((ParameterizedTypeImpl) rst_type2).getActualTypeArguments()[0];
                    return sp.getList((Class<?>)rst_type2);
                }else{
                    //是单实体
                    return sp.getItem(rst_type);
                }
            }else{
                //普通职处理
                switch (block._return){
                    case "Map":
                        return sp.getMap();
                    case "MapList":
                        return sp.getMapList();
                    default:
                        return sp.getValue();
                }
            }
        }else{
            if(block.isInsert()){
                long rst = sp.insert();
                if(Boolean.class.isAssignableFrom(rst_type)){
                    return rst>0;
                }else{
                    return rst;
                }
            }else{
                int rst = sp.execute();
                if(Boolean.class.isAssignableFrom(rst_type)){
                    return rst>0;
                }else{
                    return rst;
                }
            }
        }
    }
}
