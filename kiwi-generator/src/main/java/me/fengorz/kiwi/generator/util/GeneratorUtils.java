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

package me.fengorz.kiwi.generator.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.generator.entity.FileOutEntity;

/**
 * 自定义的代码生成器 @Author zhanshifeng @Date 2019-09-10 14:14
 */
@Slf4j
@UtilityClass
public class GeneratorUtils {

    private final Configuration CONFIG = getConfig();

    private final String[] TEMPLATE_ARR = {"controllerJavaTemplate", "entityJavaTemplate", "mapperJavaTemplate",
        "serviceJavaTemplate", "serviceImplJavaTemplate"};

    private final String OUTPUT_DIR = "outputDir";
    private final String AUTHOR = "author";
    private final String DB_URL = "dbUrl";
    private final String DRIVER_CLASS = "driverClass";
    private final String DB_USERNAME = "dbUsername";
    private final String DB_PASSWORD = "dbPassword";
    private final String PARENT_PACKAGE = "parentPackage";
    private final String MODULE_NAME = "moduleName";
    private final String MODULE_PACKAGE = "modulePackage";

    public void generatorCode() {
        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // // 全局配置
        // GlobalConfig gc = new GlobalConfig();
        //
        // gc.setOutputDir(System.getProperty("user.dir") + CONFIG.getString(OUTPUT_DIR));
        // gc.setAuthor(CONFIG.getString(AUTHOR));
        // gc.setOpen(false);
        // // gc.setSwagger2(true); 实体属性 Swagger2 注解
        // mpg.setGlobalConfig(gc);

        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl(CONFIG.getString(DB_URL));
        // dsc.setSchemaName("public");
        dsc.setDriverName(CONFIG.getString(DRIVER_CLASS));
        dsc.setUsername(CONFIG.getString(DB_USERNAME));
        dsc.setPassword(CONFIG.getString(DB_PASSWORD));
        mpg.setDataSource(dsc);

        // // 包配置
        // PackageConfig pc = new PackageConfig();
        // pc.setModuleName(CONFIG.getString(MODULE_NAME));
        // pc.setParent(CONFIG.getString(PARENT_PACKAGE));
        // mpg.setPackageInfo(pc);

        // 自定义配置
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                // to do nothing
            }
        };

        // 自定义输出配置
        List<FileOutConfig> focList = new ArrayList<>();
        List<FileOutEntity> fileOutEntitys = null;
        // List<FileOutEntity> fileOutEntitys = getFileOutEntitys(gc, pc);

        // 自定义配置会被优先输出
        fileOutEntitys.forEach(entity -> {
            focList.add(new FileOutConfig() {
                @Override
                public String outputFile(TableInfo tableInfo) {
                    entity.setEntityName(tableInfo.getEntityName());
                    return entity.toString();
                }
            });
        });

        // @Override
        // public String outputFile(TableInfo tableInfo) {
        // // 自定义输出文件名 ， 如果你 Entity 设置了前后缀、此处注意 xml 的名称会跟着发生变化！！
        // return "projectPath" + "/src/main/resources/mapper/" + pc.getModuleName()
        // + "/" + tableInfo.getEntityName() + "Mapper" + StringPool.DOT_XML;
        // }

        cfg.setFileOutConfigList(focList);
        mpg.setCfg(cfg);

        // 配置模板
        TemplateConfig templateConfig = new TemplateConfig();

        // 配置自定义输出模板
        // 指定自定义模板路径，注意不要带上.ftl/.vm, 会根据使用的模板引擎自动识别
        // templateConfig.setEntity("templates/entity2.java");
        // templateConfig.setService();
        // templateConfig.setController();

        templateConfig.setXml(null);
        mpg.setTemplate(templateConfig);

        // 策略配置
        // StrategyConfig strategy = new StrategyConfig();
        // strategy.setNaming(NamingStrategy.underline_to_camel);
        // strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        // strategy.setSuperEntityClass("com.baomidou.ant.common.BaseEntity");
        // strategy.setEntityLombokModel(true);
        // strategy.setRestControllerStyle(true);
        // 公共父类
        // strategy.setSuperControllerClass("com.baomidou.ant.common.BaseController");
        // 写于父类中的公共字段
        // strategy.setSuperEntityColumns("id");
        // strategy.setInclude(("表名，多个英文逗号分割").split(","));
        // strategy.setControllerMappingHyphenStyle(true);
        // strategy.setTablePrefix(pc.getModuleName() + "_");
        // mpg.setStrategy(strategy);
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }

    private Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            log.error("获取代码生成的配置文件失败", e);
        }
        return null;
    }

    private List<FileOutEntity> getFileOutEntitys(GlobalConfig gc, PackageConfig pc) {
        List<FileOutEntity> templateList = new ArrayList<>();
        for (String template : TEMPLATE_ARR) {
            if (StrUtil.isNotBlank(CONFIG.getString(template))) {
                FileOutEntity fileOutEntity = new FileOutEntity();
                fileOutEntity.setTemplatePath(template);
                fileOutEntity.setProjectPath(gc.getOutputDir());
                if ("mapperJavaTemplate".equals(template)) {
                    fileOutEntity.setPackagePrefix("resources/mapper/" + pc.getParent());
                } else {
                    fileOutEntity.setPackagePrefix("main/java/" + pc.getParent());
                }
                fileOutEntity.setModulePackage(CONFIG.getString(MODULE_PACKAGE));
                if ("controllerJavaTemplate".equals(template)) {
                    fileOutEntity.setModuleName(pc.getModuleName() + "/controller");
                } else if ("entityJavaTemplate".equals(template)) {
                    fileOutEntity.setModuleName(pc.getModuleName() + "/entity");
                } else if ("serviceJavaTemplate".equals(template)) {
                    fileOutEntity.setModuleName(pc.getModuleName() + "/service");
                } else if ("serviceImplJavaTemplate".equals(template)) {
                    fileOutEntity.setModuleName(pc.getModuleName() + "/service/impl");
                } else {
                    fileOutEntity.setModuleName(pc.getModuleName());
                }
                if ("mapperJavaTemplate".equals(template)) {
                    fileOutEntity.setFileSuffix(".xml");
                } else {
                    fileOutEntity.setFileSuffix(".java");
                }
            }
        }
        return templateList;
    }
}
