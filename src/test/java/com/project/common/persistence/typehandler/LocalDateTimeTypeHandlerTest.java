package com.project.common.persistence.typehandler;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalDateTimeTypeHandlerTest {

    private final LocalDateTimeTypeHandler typeHandler = new LocalDateTimeTypeHandler();

    @Test
    void readsLocalDateTimeUsingTimestamp() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        LocalDateTime expected = LocalDateTime.of(2026, 7, 29, 10, 30);
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(expected));

        LocalDateTime actual = typeHandler.getNullableResult(resultSet, "created_at");

        assertEquals(expected, actual);
        verify(resultSet).getTimestamp("created_at");
    }

    @Test
    void returnsNullWhenTimestampIsNull() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getTimestamp("created_at")).thenReturn(null);

        LocalDateTime actual = typeHandler.getNullableResult(resultSet, "created_at");

        assertNull(actual);
    }
}
