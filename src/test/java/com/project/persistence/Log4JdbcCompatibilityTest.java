package com.project.persistence;

import com.project.common.config.RootConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
class Log4JdbcCompatibilityTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void supportsJdbc41TypedGetObject() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT CURRENT_TIMESTAMP AS created_at");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();

            LocalDateTime createdAt = resultSet.getObject("created_at", LocalDateTime.class);

            assertNotNull(createdAt);
        }
    }
}
