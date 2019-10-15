package org.noear.weed.xml;

import org.noear.weed.utils.IOUtils;
import org.noear.weed.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlSqlMapperCompiler {

    public static void generator() throws Exception{
        URL path = IOUtils.getResource("/weed3/");
        File dic = new File(path.toURI());

        if (dic.isDirectory()) {
            File[] tmps = dic.listFiles();
            for (File tmp : tmps) {
                generator(tmp);
            }
        }
    }


    private static String dic_java;
    public static void generator(File xmlFile) throws Exception {
        if(dic_java == null) {
            String dic_root = IOUtils.getResource("/").toString().replace("target/classes/", "").substring(5);
            dic_java = dic_root + "src/main/java/";
        }

        JavaCodeBlock block = parse(xmlFile);
        if (block == null) {
            return;
        }

        String dic_path = dic_java + block._namespace.replace(".", "/");

        new File(dic_path).mkdirs();

        String file_path = dic_path + "/" + block._classname + ".java";
        File file = new File(file_path);
        if (!file.exists()) {
            file.createNewFile();
        }

        IOUtils.fileWrite(file, block._code);
    }

    //将xml解析为java code
    public static JavaCodeBlock parse(File xmlFile) throws Exception{
        if(xmlFile == null){
            return null;
        }

        Document doc = parseDoc(xmlFile);

        Node nm = doc.getDocumentElement();

        String namespace = attr(nm, "namespace");
        String db = attr(nm, ":db");
        String classname = xmlFile.getName().split("\\.")[0]; //namespace.replace(".","_"); //"weed_xml_sql";

        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(namespace).append(";\n\n");

        sb.append("import java.util.Map;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Collection;\n\n");

        sb.append("import org.noear.weed.DataItem;\n");
        sb.append("import org.noear.weed.DataList;\n");
        sb.append("import org.noear.weed.xml.Namespace;\n\n");


        Map<String,Node> node_map = new HashMap<>();
        NodeList sql_list = doc.getElementsByTagName("sql");
        for (int i = 0, len = sql_list.getLength(); i < len; i++) {
            Node n = sql_list.item(i);
            String id = attr(n,"id");
            if(id!=null){
                node_map.put(id,n);
            }
        }

        sb.append("@Namespace(\"").append(namespace).append("\")\n");
        sb.append("public interface ").append(classname).append("{");

        //构建block
        StringBuilder sb_tmp  =new StringBuilder();
        for (int i = 0, len = sql_list.getLength(); i < len; i++) {
            Node n = sql_list.item(i);
            XmlSqlBlock block = parseSqlNode(node_map, sb_tmp, n, namespace, db, classname);

            writerBlock(sb, block);
        }

        sb.append("\n}\n");

        node_map.clear();

        JavaCodeBlock codeBlock = new JavaCodeBlock();

        codeBlock._namespace = namespace;
        codeBlock._classname =classname;
        codeBlock._code = sb.toString();

        return codeBlock;
    }

    private static void writerBlock(StringBuilder sb, XmlSqlBlock block){
        newLine(sb,1);

        if(StringUtils.isEmpty(block._return)){
            sb.append("void");
        }
        else{
            switch (block._return){
                case "Map":
                    sb.append("Map<String,Object>");
                    break;
                case "MapList":
                    sb.append("List<Map<String,Object>>");
                    break;

                default:
                    sb.append(block._return);
                    break;
            }
        }

        sb.append(" ").append(block._id).append("(");

        block.varMap.forEach((k,v)->{
            sb.append(v.type).append(" ").append(v.name).append(",");
        });

        if(block.varMap.size()>0){
            sb.deleteCharAt(sb.length()-1);
        }

        sb.append(");");
    }

    //xml:解析 sql 指令节点
    private static XmlSqlBlock parseSqlNode(Map<String,Node> nodeMap, StringBuilder sb,Node n, String namespace, String db, String classname) {
        int depth = 1;
        XmlSqlBlock dblock = new XmlSqlBlock();

        dblock.__nodeMap = nodeMap;

        dblock._namespace = namespace;
        dblock._classname = classname;
        dblock._classcode = sb;

        dblock._id = attr(n, "id");

        dblock._declare = attr(n, ":declare");
        dblock._return = attr(n, ":return");
        if (dblock._return != null && dblock._return.indexOf("[") > 0) {
            dblock._return = dblock._return.replace("[", "<")
                                           .replace("]", ">");
        }

        String db_tmp = attr(n, ":db");
        if (StringUtils.isEmpty(db_tmp)) {
            dblock._db = db;
        } else {
            dblock._db = db_tmp;
        }


        dblock._caching = attr(n, ":caching");
        dblock._usingCache = attr(n, ":usingCache");
        dblock._cacheTag = attr(n, ":cacheTag");
        dblock._cacheClear = attr(n, ":cacheClear");

        //构建申明的变量
        _parseDeclare(dblock);

        newLine(sb, depth).append("public SQLBuilder ").append(dblock._id).append("(Map map){");

        //构建代码体和变量
        StringBuilder sb2 = new StringBuilder();
        {
            newLine(sb2, depth + 1).append("SQLBuilder sb = new SQLBuilder();\n");
            _parseNodeList(n.getChildNodes(), sb2, dblock, depth + 1);
        }

        //1.打印变量
        int var_num = 0;
        for (XmlSqlVar dv : dblock.varMap.values()) {
            if (dv.type != null && dv.type.length() > 0) {
                var_num++;
                newLine(sb, depth + 1)
                        .append(dv.type).append(" ").append(dv.name).append("=")
                        .append("(").append(dv.type).append(")map.get(\"").append(dv.name).append("\");");
            }
        }

        if (var_num > 0) {
            sb.append("\n");
        }

        //2.打印代码体
        sb.append(sb2);

        sb.append("\n");
        newLine(sb, depth + 1).append("return sb;");
        newLine(sb, depth).append("}\n");

        dblock.__nodeMap = null;
        //注册块
        return dblock;
    }

    private static void _parseDeclare(XmlSqlBlock dblock) {
        if (dblock._declare == null) {
            return;
        }
        String[] ss = dblock._declare.split(",");
        for (int i = 0, len = ss.length; i < len; i++) {
            String tmp = ss[i].trim();
            if (tmp.indexOf(":") > 0 && tmp.length() > 3) {
                String[] kv = tmp.split(":");

                XmlSqlVar dv = new XmlSqlVar(tmp, kv[0].trim(), kv[1].trim());
                dblock.varPut(dv);
            }
        }
    }

    private static void _parseNodeList(NodeList nl, StringBuilder sb, XmlSqlBlock dblock, int depth) {
        for (int i = 0, len = nl.getLength(); i < len; i++) {
            Node n = nl.item(i);

            _parseNode(n,sb,dblock,depth);
        }
    }

    private static void _parseNode(Node n, StringBuilder sb, XmlSqlBlock dblock,  int depth){
        int type = n.getNodeType();

        if (type == 3) {//text
            String text = n.getTextContent().trim();

            if (text.length() > 0) {
                newLine(sb, depth).append("sb.append(");
                parseTxt(sb,dblock,text);
                sb.append(");");
            }
        }

        if (type == 1) {//elem
            String tagName = n.getNodeName();

            if ("if".equals(tagName)) {
                parseIfNode(sb,dblock,n,depth);
                return;
            }


            if("for".equals(tagName)){
                parseForNode(sb,dblock,n,depth);
                return;
            }

            if("ref".equals(tagName)){
                parseRefNode(sb,dblock,n,depth);
                return;
            }

            _parseNodeList(n.getChildNodes(),sb,dblock,depth);
        }
    }

    //xml:解析 if 指令节点
    private static void parseIfNode(StringBuilder sb, XmlSqlBlock dblock, Node n , int depth) {
        String _test = attr(n, "test");

        newLine(sb, depth).append("if(").append(_test).append("){");

        _parseNodeList(n.getChildNodes(), sb, dblock, depth + 1);

        newLine(sb, depth).append("}");
    }

    //xml:解析 ref 指令节点
    private static void parseRefNode(StringBuilder sb, XmlSqlBlock dblock, Node n , int depth) {
        String _sql_id = attr(n, "sql");
        if (StringUtils.isEmpty(_sql_id) == false) {
            Node ref_n = dblock.__nodeMap.get(_sql_id);
            if (ref_n == null) {
                throw new RuntimeException("sql node @" + _sql_id + " can't find");
            }
            _parseNode(ref_n, sb, dblock, depth);
        }
    }

    //xml:解析 for 指令节点
    private static void parseForNode(StringBuilder sb, XmlSqlBlock dblock, Node n , int depth) {
        String _var_str = attr(n, "var").trim();

        if (_var_str.indexOf(":") < 0 || _var_str.length() < 3) {
            StringBuilder eb = new StringBuilder();
            eb.append(dblock._namespace).append("/").append(dblock._id).append("::")
                    .append("for/var(").append(_var_str).append(") must declare the type");
            throw new RuntimeException(eb.toString());
        }

        String[] kv = _var_str.split(":");

        XmlSqlVar _var = new XmlSqlVar(_var_str, kv[0].trim(), kv[1].trim());
        String _items = attr(n, "items");

        //newLine(sb, depth).append("Iterable<").append(_var.type).append("> ").append(_items).append("=(Iterable<").append(_var.type).append(">)map.get(\"").append(_items).append("\");");
        newLine(sb, depth).append("for(").append(_var.type).append(" ").append(_var.name).append(" : ").append(_items).append("){");

        _parseNodeList(n.getChildNodes(), sb, dblock, depth + 1);

        //注到
        XmlSqlVar _itemsVar = new XmlSqlVar(_items, _items, "Collection<" + _var.type + ">");
        dblock.varPut(_itemsVar);

        newLine(sb, depth).append("}");
    }

    //sb:新起一行代码
    private static StringBuilder newLine(StringBuilder sb, int depth){
        sb.append("\n");
        while (depth>0){
            sb.append("  ");
            depth--;
        }

        return sb;
    }

    //xml:读取属性
    private static String attr(Node n, String name){
        Node tmp = n.getAttributes().getNamedItem(name);
        if(tmp == null){
            return null;
        }else{
            return tmp.getNodeValue();
        }
    }

    private static DocumentBuilderFactory dbf = null;
    private static DocumentBuilder db = null;
    //xml:解析文档
    private static Document parseDoc(File xmlFile) throws Exception{
        if(dbf ==null) {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
        }

        return db.parse(xmlFile);
    }

    //sql::格式化字符串
    private static void parseTxt(StringBuilder sb, XmlSqlBlock dblock, String txt){
        String txt2 = null;
        Map<String, XmlSqlVar> tmpList = new LinkedHashMap<>();

        //0.确定动作
        if(dblock.action==null){
            txt2 = txt.trim().toUpperCase();

            if(txt2.startsWith("INSERT")){
                dblock.action = "INSERT";
            }

            if(txt2.startsWith("DELETE")){
                dblock.action = "DELETE";
            }

            if(txt2.startsWith("UPDATE")){
                dblock.action = "UPDATE";
            }

            if(txt2.startsWith("SELECT")){
                dblock.action = "SELECT";
            }
        }

        txt2 = txt;
        //1.处理${xxx},${xxx,type}
        {
            tmpList.clear();

            Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
            Matcher m = pattern.matcher(txt2);

            while (m.find()) {
                XmlSqlVar dv = new XmlSqlVar();
                dv.mark = m.group(0);
                dv.name = m.group(1).trim();
                if (dv.name.indexOf(":") > 0) {
                    String[] kv = dv.name.split(":");
                    dv.name = kv[0].trim();
                    dv.type = kv[1].trim();
                }

                tmpList.put(dv.name, dv);
                dblock.varPut(dv);
            }

            for (XmlSqlVar dv : tmpList.values()) {
                txt2 = txt2.replace(dv.mark, "\"+" + dv.name + "+\"");
            }
        }

        //2.找出@{xxx},@{xxx:type}
        {
            tmpList.clear();

            Pattern pattern = Pattern.compile("@\\{(.+?)\\}");
            Matcher m = pattern.matcher(txt2);
            while (m.find()) {
                XmlSqlVar dv = new XmlSqlVar();
                dv.mark = m.group(0);
                dv.name = m.group(1).trim();
                if (dv.name.indexOf(":") > 0) {
                    String[] kv = dv.name.split(":");
                    dv.name = kv[0].trim();
                    dv.type = kv[1].trim();
                }

                tmpList.put(dv.name, dv);
                dblock.varPut(dv);
            }

            for (XmlSqlVar dv : tmpList.values()) {
                txt2 = txt2.replace(dv.mark, "?");
            }
            sb.append("\"").append(txt2).append(" \"");
            tmpList.forEach((k, v) -> {
                sb.append(",").append(v.name);
            });
        }
    }
}