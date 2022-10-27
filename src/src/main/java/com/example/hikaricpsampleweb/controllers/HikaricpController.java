package com.example.hikaricpsampleweb.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
        //config.setRegisterMbeans(true);
        config.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(meterRegistry));
        ds = new HikariDataSource(config);

        return "Ok";
    }

    @RequestMapping(method=RequestMethod.GET)
    public String get() {
        log.info("Get");

        if (ds == null) {
            return "Err";
        }

        try (final Connection conn = ds.getConnection();
             final PreparedStatement statement = conn.prepareStatement("SELECT 1")) {
            log.info("Query");
            statement.executeQuery();
            log.info("Done");
            conn.close();
        } catch (SQLException e) {
            log.error(e.getMessage());
            log.error(e.getSQLState());
            return "Err";
        }

        return "Ok";
    }
}
