package com.dyh.transaction.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author dyh
 * @created at 2017 10 30 11:33
 */
@Configuration
public class DataSourceConfig {

    /**
     * 多个package以 ;,\t\n 中的任何一个字符分割
     */
    static final String PACKAGE = "com.dyh.transaction.dao.mapper";

    @Primary
    @Bean(name = "transactionDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.transaction")
    public DataSource transactionDataSource() {
        return new DruidDataSource();
    }

    @Primary
    @Bean(name = "transactionTransactionManager")
    public DataSourceTransactionManager transactionTransactionManager(@Qualifier("transactionDataSource") DataSource transactionDataSource) {
        return new DataSourceTransactionManager(transactionDataSource);
    }

    @Primary
    @Bean(name = "transactionSqlSessionFactory")
    public SqlSessionFactory transactionSqlSessionFactory(@Qualifier("transactionDataSource") DataSource transactionDataSource) throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setVfs(SpringBootVFS.class);

        sessionFactory.setDataSource(transactionDataSource);
        //mybatis分页
        Properties props = new Properties();
        props.setProperty("dialect", "mysql");
        props.setProperty("reasonable", "true");
        props.setProperty("supportMethodsArguments", "true");
        props.setProperty("returnPageInfo", "check");
        props.setProperty("arg", "count=countSql");

        sessionFactory.setTypeHandlersPackage("com.dyh.transaction.dao.typehandler");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath*:/mybatis/*.xml"));
        return sessionFactory.getObject();
    }

    @Primary
    @Bean
    public MapperScannerConfigurer transactionMapperScannerConfigurer(){
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage(PACKAGE);
        configurer.setSqlSessionFactoryBeanName("transactionSqlSessionFactory");
        return configurer;
    }
}
