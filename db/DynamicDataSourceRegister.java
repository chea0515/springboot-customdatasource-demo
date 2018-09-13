package com.cc.bms.base.config.db;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DynamicDataSourceRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    // 默认数据源
    private static final Object DEFAULT_DATASOURCE_TYPE = "org.apache.tomcat.jdbc.pool.DataSource";
    private DataSource defaultDataSource;
    private Map<String, DataSource> customDataSources;

    private ConversionService defaultConversionService = new DefaultConversionService();
    private PropertyValues defaultDataSourceValues;

    @Override
    public void setEnvironment(Environment environment) {
        initDefaultDataSource(environment);
        initCustomDataSource(environment);
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        Map<String, Object> targetDataSources = new HashMap<String, Object>();
        targetDataSources.put("dataSource", defaultDataSource);
        DynamicDataSourceContextHolder.dataSourcesIds.add("dataSource");
        targetDataSources.putAll(customDataSources);
        for (String key : customDataSources.keySet()) {
            DynamicDataSourceContextHolder.dataSourcesIds.add(key);
        }

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);

        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        propertyValues.addPropertyValue("defaultTargetDataSource", defaultDataSource);
        propertyValues.add("targetDataSources", targetDataSources);
        beanDefinitionRegistry.registerBeanDefinition("dataSource", beanDefinition);
    }

    /**
     * 加载默认数据源
     * @param environment Environment.
     */
    private void initDefaultDataSource(Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "spring.datasource.");
        Map<String, Object> dataSourceMap = new HashMap<String, Object>();

        dataSourceMap.put("type", propertyResolver.getProperty("type"));
        dataSourceMap.put("driverClassName", propertyResolver.getProperty("driverClassName"));
        dataSourceMap.put("url", propertyResolver.getProperty("url"));
        dataSourceMap.put("username", propertyResolver.getProperty("username"));
        dataSourceMap.put("password", propertyResolver.getProperty("password"));

        defaultDataSource = buildDataSource(dataSourceMap);
        dataBinder(defaultDataSource, environment);
    }

    private void initCustomDataSource(Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "my.datasource.");
        String[] dataSourcePrefixs = propertyResolver.getProperty("names").split(",");
        Map<String, Object> dataSourceMap;
        DataSource dataSource;
        customDataSources = new HashMap<String, DataSource>();

        for(String dataSourcePrefix : dataSourcePrefixs) {
            dataSourceMap = propertyResolver.getSubProperties(dataSourcePrefix + ".");
            dataSource = buildDataSource(dataSourceMap);
            customDataSources.put(dataSourcePrefix, dataSource);
            dataBinder(dataSource, environment);
        }
    }

    @SuppressWarnings({"unchecked"})
    private DataSource buildDataSource(Map<String, Object> dataSourceMap) {
        Object type = dataSourceMap.get("type");
        if (null == type) {
            type = DEFAULT_DATASOURCE_TYPE;
        }

        Class<? extends DataSource> dataSourceType;
        try {
            dataSourceType = (Class<? extends DataSource>) Class.forName((String) type);

            String driverClassName = dataSourceMap.get("driverClassName").toString();
            String url = dataSourceMap.get("url").toString();
            String username = dataSourceMap.get("username").toString();
            String password = dataSourceMap.get("password").toString();

            DataSourceBuilder factory = DataSourceBuilder.create()
                    .type(dataSourceType)
                    .driverClassName(driverClassName)
                    .url(url)
                    .username(username)
                    .password(password);
            return factory.build();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void dataBinder(DataSource dataSource, Environment environment) {
        RelaxedDataBinder dataBinder = new RelaxedDataBinder(dataSource);
        dataBinder.setConversionService(defaultConversionService);
        dataBinder.setIgnoreNestedProperties(false);
        dataBinder.setIgnoreInvalidFields(false);
        dataBinder.setIgnoreUnknownFields(true);

        if (null == defaultDataSourceValues) {
            Map<String, Object> propertyResolverMap =
                    new RelaxedPropertyResolver(environment, "spring.datasource").getSubProperties(".");
            Map<String, Object> newPropertyResolverMap = new HashMap<String, Object>(propertyResolverMap);

            newPropertyResolverMap.remove("type");
            newPropertyResolverMap.remove("driverClassName");
            newPropertyResolverMap.remove("url");
            newPropertyResolverMap.remove("username");
            newPropertyResolverMap.remove("password");

            defaultDataSourceValues = new MutablePropertyValues(newPropertyResolverMap);
            dataBinder.bind(defaultDataSourceValues);
        }
    }
}
