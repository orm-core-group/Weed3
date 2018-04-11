package noear.weed;

import noear.weed.ext.Act2;

import javax.swing.plaf.TextUI;
import java.io.*;
import java.util.*;

/**
 * Created by yuety on 14-9-10.
 */
public class DataItem implements IDataItem{
    HashMap<String,Object> _data = new HashMap<>();
    List<String> _keys = new ArrayList<>();

    public DataItem() { }
    public DataItem(Boolean isUsingDbNull) { _isUsingDbNull = isUsingDbNull; }

    public int count(){
        return _data.size();
    }
    public void clear(){
        _data.clear();
    }
    public boolean exists(String name){
        return _data.containsKey(name);
    }
    public List<String> keys(){
        return _keys;
    }

    public IDataItem set(String name,Object value)
    {
        _data.put(name, value);
        _keys.add(name);
        return this;
    }
    public Object get(int index){
        return get(_keys.get(index));
    }
    public Object get(String name){
        return _data.get(name);
    }
    public Variate getVariate(String name)
    {
        if (_data.containsKey(name))
            return new Variate(name, get(name));
        else
            return new Variate(name, null);
    }

    public void remove(String name){
        _data.remove(name);
    }

    public <T extends IBinder> T toItem(T item)
    {
        item.bind((key) -> getVariate(key));

        return item;
    }

    public short getShort(String name){
        return (short)get(name);
    }

    public int getInt(String name){
        return (int)get(name);
    }

    public long getLong(String name){
        return (long)get(name);
    }

    public double getDouble(String name){
        return (double)get(name);
    }

    public float getFloat(String name){
        return (float)get(name);
    }

    public String getString(String name){
        return (String)get(name);
    }

    public boolean getBoolean(String name){
        return (boolean)get(name);
    }

    public Date getDateTime(String name){
        return (Date)get(name);
    }

    public void forEach(Act2<String, Object> callback)
    {
        for(Map.Entry<String,Object> kv : _data.entrySet()){
            Object val = kv.getValue();

            if(val == null && _isUsingDbNull){
                callback.run(kv.getKey(), "$NULL");
            }else {
                callback.run(kv.getKey(), val);
            }
        }
    }

    private boolean _isUsingDbNull=false;

    //============================
    public static IDataItem create(IDataItem schema, GetHandler source) {
        DataItem item = new DataItem();
        for (String key : schema.keys()) {
            Object val = source.get(key);
            if (val != null) {
                item.set(key, val);
            }
        }
        return item;
    }

    public String toJson(){
        _JsonWriter jw = new _JsonWriter();

        buildJson(jw);

        return jw.toJson();
    }

    protected void buildJson(_JsonWriter jw){
        jw.WriteObjectStart();
        for(String key : keys()){
            Object val = get(key);

            jw.WritePropertyName(key);

            if(val == null)
                jw.WriteNull();

            if(val instanceof String)
                jw.WriteValue((String)val);

            if(val instanceof Date)
                jw.WriteValue((Date)val);

            if(val instanceof Boolean)
                jw.WriteValue((Boolean)val);

            if(val instanceof Integer)
                jw.WriteValue((Integer)val);

            if(val instanceof Long)
                jw.WriteValue((Long)val);

            jw.WriteValue(new Variate(null, val).doubleValue(0));

        }
        jw.WriteObjectEnd();
    }

    public String serialize() throws Exception {
        String data = null;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(byteStream);
            try {
                out.writeObject(this);
                data = byteStream.toString("ISO-8859-1");//必须是ISO-8859-1

            } finally {
                out.close();
            }
        }finally {
            byteStream.close();
        }

        return data;
    }

    public String trySerialize(){
        try{
            return serialize();
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static DataItem unserialize(String data) throws Exception{
        if(data == null){
            return null;
        }

        DataItem item = null;
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data.getBytes("ISO-8859-1"));
        try {
            ObjectInputStream inp = new ObjectInputStream(byteStream);
            try {
                item = (DataItem) inp.readObject();
            } finally {
                byteStream.close();
            }
        }finally {
            byteStream.close();
        }

        return item;
    }

    public static DataItem tryUnserialize(String data){
        try{
            return unserialize(data);
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }


}
