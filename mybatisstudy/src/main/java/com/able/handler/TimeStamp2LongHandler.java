package com.able.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;


/**
 * @param
 * @author jipeng
 * @date 2021-02-08 14:49
 */
public class TimeStamp2LongHandler extends BaseTypeHandler<Long> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType) throws SQLException {

       ps.setTimestamp(i,new Timestamp(parameter));
    }

    @Override
    public Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getDate(columnName).getTime();
    }

    @Override
    public Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getDate(columnIndex).getTime();
    }

    @Override
    public Long getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getDate(columnIndex).getTime();
    }
}

