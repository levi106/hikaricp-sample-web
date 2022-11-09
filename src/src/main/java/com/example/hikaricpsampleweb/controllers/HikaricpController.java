package com.example.hikaricpsampleweb.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
// import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("hikaricp")
public class HikaricpController {
    private HikariDataSource ds = null;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @RequestMapping(value="/init", method=RequestMethod.POST)
    public String init() {
        log.info("Init");

        if (ds != null) {
            ds.close();
        }

        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        final String connectionTimeoutMs = System.getenv("CONNECTION_TIMEOUT_MS");
        if (connectionTimeoutMs != null && !connectionTimeoutMs.isEmpty()) {
            config.setConnectionTimeout(Integer.parseInt(connectionTimeoutMs));
        }
        final String maxPoolSize = System.getenv("MAX_POOL_SIZE");
        if (maxPoolSize != null && !maxPoolSize.isEmpty()) {
            config.setMaximumPoolSize(Integer.parseInt(maxPoolSize));
        }
        final String maxLifetimeMs = System.getenv("MAX_LIFETIME_MS");
        if (maxLifetimeMs != null && !maxLifetimeMs.isEmpty()) {
            config.setMaxLifetime(Integer.parseInt(maxLifetimeMs));
        }
        final String validationTimeoutMs = System.getenv("VALIDATION_TIMEOUT_MS");
        if (validationTimeoutMs != null && !validationTimeoutMs.isEmpty()) {
            config.setValidationTimeout(Integer.parseInt(validationTimeoutMs));
        }
        config.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(meterRegistry));
        ds = new HikariDataSource(config);

        return "Ok";
    }

    private String process(Integer millis) {
        if (ds == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not initialized.");
        }

        try (final Connection conn = ds.getConnection();
             final PreparedStatement statement = conn.prepareStatement("SELECT 1")) {
            log.info("Query: " + conn.toString());
            statement.executeQuery();
            log.info("Sleep: {} sec", millis);
            Thread.sleep(millis);
            log.info("Done");
            conn.close();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Err");
        } catch (SQLException e) {
            log.error(e.getMessage());
            log.error(e.getSQLState());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Err");
        }

        return "Ok";
    }

    @RequestMapping(path="/{millis}", method=RequestMethod.GET)
    public String get(@PathVariable Integer millis) {
        log.info("Get: {}", millis);
        return process(millis);
    }

    @RequestMapping(method=RequestMethod.GET)
    public String get() {
        log.info("Get");
        return process(0);
    }
}
