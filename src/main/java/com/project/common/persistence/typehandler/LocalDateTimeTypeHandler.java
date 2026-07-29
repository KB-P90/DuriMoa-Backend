package com.project.common.persistence.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            LocalDateTime parameter,
            JdbcType jdbcType) throws SQLException {
        statement.setTimestamp(index, Timestamp.valueOf(parameter));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet resultSet, String columnName)
            throws SQLException {
        return toLocalDateTime(resultSet.getTimestamp(columnName));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet resultSet, int columnIndex)
            throws SQLException {
        return toLocalDateTime(resultSet.getTimestamp(columnIndex));
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement statement, int columnIndex)
            throws SQLException {
        return toLocalDateTime(statement.getTimestamp(columnIndex));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
