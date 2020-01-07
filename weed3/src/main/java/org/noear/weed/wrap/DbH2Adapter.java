package org.noear.weed.wrap;

import java.sql.Clob;
import java.sql.SQLException;

public class DbH2Adapter implements DbAdapter{
    //top,page 和mysql一样

    @Override
    public Object preChange(Object val) throws SQLException {
        if(val instanceof Clob){
            Clob clob = ((Clob) val);
            return clob.getSubString(1,(int)clob.length());
        } else if(val instanceof Byte){
            return ((Byte)val).byteValue() > 0;
        } else{
            return val;
        }
    }
}
