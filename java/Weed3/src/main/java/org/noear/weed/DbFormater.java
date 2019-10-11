package org.noear.weed;

import org.noear.weed.utils.StringUtils;

public class DbFormater {
    //字段格式符
    private String _fieldFormat;
    private String _fieldFormat_start;
    //对象格式符
    private String _objectFormat;
    private String _objectFormat_start;

    /**
     * 字段格式符设置
     */
    public void fieldFormatSet(String format) {
        _fieldFormat = format;
        if (format != null && format.length() > 1) {
            _fieldFormat_start = format.substring(0, 1);
        } else {
            _fieldFormat_start = "";
        }
    }

    /**
     * 对象格式符设置
     */
    public void objectFormatSet(String format) {
        _objectFormat = format;
        if (format != null && format.length() > 1) {
            _objectFormat_start = format.substring(0, 1);
        } else {
            _objectFormat_start = "";
        }
    }


    /**
     * 字段格式化（用于：set(..,v)）
     */
    public String formatField(String name) {
        if (StringUtils.isEmpty(_fieldFormat)) {
            return name;
        }

        if (name.startsWith(_fieldFormat_start)) {
            return name;
        }

        return _fieldFormat.replace("%", name);
    }


    /**
     * 字段格式化（用于：select(..) orderBy(..)）
     */
    public String formatColumns(String columns) {
        if (StringUtils.isEmpty(_fieldFormat)) {
            return columns;
        }

        StringBuilder sb = StringUtils.borrowBuilder();
        String[] ss = columns.split(",");

        for(int i=0,len=ss.length; i<len; i++){
            String name = ss[i].trim();

            if(name.indexOf(" ")>0){
                String[] ss2 = name.split(" ");
                //类假：xxx_name name; name ASC
                if(ss2.length==2) {
                    sb.append(format_column_do(ss2[0])).append(" ").append(ss2[1]).append(",");
                }else{
                    sb.append(name).append(",");
                }
            }else{
                sb.append(format_column_do(name)).append(",");
            }
        }

        if(sb.length()>0){
            sb.deleteCharAt(sb.length()-1);
        }

        return StringUtils.releaseBuilder(sb);
    }


    private String format_column_do(String name){
        if (name.startsWith(_fieldFormat_start) || name.indexOf(".") > 0 || name.indexOf("(") > 0) {
            return name;
        }

        return _fieldFormat.replace("%", name);
    }

    /**
     * 对象格式化（用于：from(..), join(..)）
     */
    public String formatObject(String name) {
        if (StringUtils.isEmpty(_objectFormat)) {
            return name;
        }

        if (name.startsWith(_objectFormat_start) || name.indexOf(".") > 0) {
            return name;
        }

        if (name.indexOf(" ") < 0) {
            return _objectFormat.replace("%", name);
        }

        String[] ss = name.split(" ");

        if (ss.length != 2) {
            return name;
        }

        StringBuilder sb = StringUtils.borrowBuilder();

        sb.append(_objectFormat.replace("%", ss[0]))
                .append(" ")
                .append(ss[1]);

        return StringUtils.releaseBuilder(sb);
    }
}
