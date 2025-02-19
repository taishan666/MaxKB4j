package com.tarzan.maxkb4j.config;

import org.springframework.context.annotation.Configuration;

//@Configuration
//@EnableCaching
public class CacheConfig {


  /*  @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            cm.createCache("books",
                    new MutableConfiguration<>()
                            .setTypes(Object.class, Object.class)
                            .setExpiryPolicyFactory(
                                    AccessedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES)
                            )
                            .setStatisticsEnabled(true)
            );
        };
    }*/
}