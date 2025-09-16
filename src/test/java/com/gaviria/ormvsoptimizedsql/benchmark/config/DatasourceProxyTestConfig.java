// src/test/java/.../config/DatasourceProxyTestConfig.java
package com.gaviria.ormvsoptimizedsql.benchmark.config;

import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class DatasourceProxyTestConfig {

    @Bean
    public QueryExecutionListener queryCountListener() {
        return new DataSourceQueryCountListener();
    }
}