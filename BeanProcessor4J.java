package com.easyUI.utils;

import java.beans.PropertyDescriptor;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
//转载自 http://blog.csdn.net/shehun11/article/details/29569161
//通过装饰者模式,覆盖beanProcessor处理数据库字段与bean字段不同且可以映射
public class BeanProcessor4J extends BeanProcessor{

        @Override
        protected int[] mapColumnsToProperties(ResultSetMetaData rsmd, PropertyDescriptor[] props) throws SQLException {
                int cols = rsmd.getColumnCount();
                int[] columnToProperty = new int[cols + 1];
                Arrays.fill(columnToProperty, PROPERTY_NOT_FOUND);
                for (int col = 1; col <= cols; col++) {
                        String columnName = rsmd.getColumnLabel(col);
                        if (null == columnName || 0 == columnName.length()) {
                                columnName = rsmd.getColumnName(col);
                        }
                        for (int i = 0; i< props.length; i++) {
                                if (convert(columnName).equals(props[i].getName())) {
                                        columnToProperty[col] = i;
                                        break;
                                }
                        }
                }
                return columnToProperty;
        }

        /**
         * DATA_OBJECT_NAME -> dataObjectName
         */
        private String convert(String objName) {
                StringBuilder result = new StringBuilder();
                String[] tokens = objName.split("_");
                for (String token : tokens) {
                        if (result.length() == 0)
                                result.append(token.toLowerCase());
                        else
                                result.append(StringUtils.capitalize(token.toLowerCase()));
                }
               // System.out.println(result.toString());
                return result.toString();
        }
}