package com.aicc.silverlink.global.config;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;

@Configuration
public class DataSourceProxyConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource && !(bean instanceof net.ttddyy.dsproxy.support.ProxyDataSource)) {
            // DataSource가 초기화된 후 프록시로 감싸서 반환함
            return ProxyDataSourceBuilder.create((DataSource) bean)
                    .name("MyProxyDataSource")
                    .logQueryBySlf4j(SLF4JLogLevel.INFO) // 로그 레벨 설정
                    .multiline() // 예쁘게 여러 줄로 출력
                    .countQuery() // 쿼리 카운트 기능 활성화
                    .build();
        }
        return bean;
    }
}