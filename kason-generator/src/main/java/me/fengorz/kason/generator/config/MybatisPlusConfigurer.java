/*
 *
 * Copyright [2019~2025] [zhanshifeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kason.generator.config;

import javax.sql.DataSource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author lengleng
 * @date 2019/2/1
 */
@Configuration
@MapperScan("me.fengorz.kason.generator.mapper")
@ComponentScan("me.fengorz.kason.generator")
public class MybatisPlusConfigurer {

    /**
     * 分页插件
     *
     * @return PaginationInterceptor
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }

    @Bean
    public DataSource dataSource() throws ConfigurationException {
        PropertiesConfiguration properties = new PropertiesConfiguration("generator.properties");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getString("dbUrl"));
        dataSource.setDriverClassName(properties.getString("driverClass"));
        dataSource.setUsername(properties.getString("dbUsername"));
        dataSource.setPassword(properties.getString("dbPassword"));
        return dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("classpath:/mapper/**/*.xml"));

        // 指定扫描别名包的路径，多个bean的扫描路径，拼接以分号隔开
        // String typeAliasesPackage = "com.wzh.demo.domain;";
        // sqlSessionFactoryBean.setTypeAliasesPackage(typeAliasesPackage);

        return sqlSessionFactoryBean.getObject();
    }

    // 创建事物管理器
    @Bean
    public PlatformTransactionManager transactionManager() throws ConfigurationException {
        return new DataSourceTransactionManager(dataSource());
    }
}
