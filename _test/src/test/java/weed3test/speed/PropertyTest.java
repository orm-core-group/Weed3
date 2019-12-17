package weed3test.speed;

import org.junit.Test;
import org.noear.weed.utils.Prop;
import weed3test.model.AppxModel;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PropertyTest {

    @Test
    public void test1() {
        Prop<AppxModel, ?> p1 = AppxModel::getApp_id;
        Prop<AppxModel, ?> p2 = AppxModel::getAgroup_id;
        Prop<AppxModel, ?> p3 = AppxModel::getApp_key;

        System.out.println(getName(p1));
        System.out.println(getName(p2));
        System.out.println(getName(p3));

        long start = System.currentTimeMillis();
        for(int i=0 ; i< 100000 ; i++){
            getName(p1);
            getName(p2);
            getName(p3);
        }
        long times = System.currentTimeMillis() - start;

        System.out.println("用时："+times);
    }

    @Test
    public void test2() {
        Prop<AppxModel, ?> p1 = AppxModel::getApp_id;
        Prop<AppxModel, ?> p2 = AppxModel::getAgroup_id;
        Prop<AppxModel, ?> p3 = AppxModel::getApp_key;

        System.out.println(getName2(p1));
        System.out.println(getName2(p2));
        System.out.println(getName2(p3));

        long start = System.currentTimeMillis();
        for(int i=0 ; i< 100000 ; i++){
            getName2(p1);
            getName2(p2);
            getName2(p3);
        }
        long times = System.currentTimeMillis() - start;

        System.out.println("用时："+times);
    }

    private static Map<Prop,String> _cache = new ConcurrentHashMap<>();

    private static  <C> String getName2(Prop<C, ?> property){
        String tmp = _cache.get(property);
        if(tmp == null){
            tmp = getName(property);
            _cache.putIfAbsent(property,tmp);
        }

        return tmp;
    }

    private static  <C> String getName(Prop<C, ?> property) {
        try {
            Method declaredMethod = property.getClass().getDeclaredMethod("writeReplace");
            declaredMethod.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda) declaredMethod.invoke(property);
            String method = serializedLambda.getImplMethodName();
            String attr = null;
            if (method.startsWith("get")) {
                attr = method.substring(3);
            } else {
                attr = method.substring(2);
            }
            return attr;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
