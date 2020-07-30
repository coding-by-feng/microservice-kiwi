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

package me.fengorz.kiwi.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.baomidou.mybatisplus.core.toolkit.Constants;

import me.fengorz.kiwi.common.sdk.util.time.KiwiDateFormatUtils;
import me.fengorz.kiwi.common.sdk.util.time.KiwiDateUtils;
import me.fengorz.kiwi.generator.common.ToolConstants;
import me.fengorz.kiwi.generator.entity.ColumnEntity;
import me.fengorz.kiwi.generator.entity.GenerateAbility;
import me.fengorz.kiwi.generator.entity.GenerateConfig;
import me.fengorz.kiwi.generator.entity.TableEntity;
import me.fengorz.kiwi.generator.util.ToolBeanUtils;
import me.fengorz.kiwi.generator.util.ToolIOUtils;

/**
 * @Author zhanshifeng
 * @Date 2019-09-16 16:33
 */
public class CustomCodeGenerator {

    private static final String TEMPLATES = "templates/";
    private static final String ENTITY_JAVA_VM = "Entity.java.vm";
    private static final String ENTITY_COLUMN_JAVA_VM = "EntityColumn.java.vm";
    private static final String MAPPER_JAVA_VM = "Mapper.java.vm";
    private static final String SERVICE_JAVA_VM = "Service.java.vm";
    private static final String SERVICE_IMPL_JAVA_VM = "ServiceImpl.java.vm";
    private static final String CONTROLLER_JAVA_VM = "Controller.java.vm";
    private static final String REMOTE_SERVICE_JAVA_VM = "RemoteService.java.vm";
    private static final String REMOTE_SERVICE_FALLBACK_IMPL_JAVA_VM = "RemoteServiceFallBackImpl.java.vm";
    private static final String REMOTE_SERVICE_FALLBACK_FACTORY_JAVA_VM = "RemoteServiceFallBackFactory.java.vm";
    private static final String MAPPER_XML_VM = "Mapper.xml.vm";
    private static final String VO_JAVA_VM = "VO.java.vm";
    private static final String DTO_JAVA_VM = "DTO.java.vm";

    public static void generatorCode(GenerateAbility generateAbility, GenerateConfig generateConfig,
        Map<String, String> table, List<Map<String, String>> columns) throws Exception {
        boolean hasBigDecimal = false;
        // 表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        // 表名转换成Java类名
        String className =
            ToolBeanUtils.firstUpperCamelCase(tableEntity.getTableName(), generateConfig.getTablePreName());
        tableEntity.setCaseTableName(className);
        tableEntity.setLowerTableName(StringUtils.uncapitalize(className));

        // 列信息
        List<ColumnEntity> columnList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setColumnNameUpper(column.get("columnName").toUpperCase());
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));
            columnEntity.setIsNullable(column.get("isNullable"));
            columnEntity.setColumnDefault(column.get("columnDefault"));

            // 列名转换成Java属性名
            String attrName = ToolBeanUtils.defaultColumnToBeanProperty(columnEntity.getColumnName());
            columnEntity.setCaseAttrName(attrName);
            columnEntity.setLowerAttrName(StringUtils.uncapitalize(attrName));

            // 列的数据类型，转换成Java类型
            String attrType = ToolConstants.DATA_TYPE_MAP.get(columnEntity.getDataType());
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && "BigDecimal".equals(attrType)) {
                hasBigDecimal = true;
            }
            // 是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columnList.add(columnEntity);
        }
        tableEntity.setColumns(columnList);

        // 没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        // 设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        // 封装模板数据
        Map<String, Object> map = new HashMap<>(16);
        map.put("tableName", tableEntity.getTableName());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getCaseTableName());
        map.put("classname", tableEntity.getLowerTableName());
        map.put("pathName", tableEntity.getLowerTableName().toLowerCase());
        String controllerRootPath = getControllerRootPath(tableEntity.getTableName());
        map.put("controllerRootPath", controllerRootPath);
        map.put("tableNameUpper", tableEntity.getTableName().toUpperCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("datetime", KiwiDateFormatUtils.format(new Date(), KiwiDateUtils.DEFAULT_TIME_PATTERN));
        map.put("comments", tableEntity.getComments());
        map.put("author", "zhanShiFeng");
        map.put("moduleName", generateConfig.getModuleName());
        map.put("package", generateConfig.getPackageName());
        map.put("serviceId", generateConfig.getServiceId());
        map.put("caseTableName", tableEntity.getCaseTableName());
        map.put("serialVersionUID", System.currentTimeMillis());
        map.put("voSerialVersionUID", System.currentTimeMillis() + 168);
        map.put("dtoSerialVersionUID", System.currentTimeMillis() + 188);
        VelocityContext context = new VelocityContext(map);

        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(generateConfig.getZipPath()));

        // 获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, ToolConstants.UTF_8);
            tpl.merge(context, sw);

            try {
                // 添加到zip
                String fileName = getFileName(generateAbility, template, tableEntity.getCaseTableName(),
                    map.get("package").toString(), map.get("moduleName").toString());
                if (StringUtils.isBlank(fileName)) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(fileName));
                // TODO: 2020/2/24
                ToolIOUtils.write(zip, ToolConstants.UTF_8, false, sw.toString());
                ToolIOUtils.close(sw);
                zip.closeEntry();
                System.out.println("=========>file[" + fileName + "] generate success!");
            } catch (IOException e) {
                throw new Exception("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
        zip.close();
    }

    private static List<String> getTemplates() {
        List<String> templates = new ArrayList<>();
        templates.add("templates/Entity.java.vm");
        templates.add("templates/VO.java.vm");
        templates.add("templates/DTO.java.vm");
        templates.add("templates/EntityColumn.java.vm");
        templates.add("templates/Mapper.java.vm");
        templates.add("templates/Mapper.xml.vm");
        templates.add("templates/Service.java.vm");
        templates.add("templates/ServiceImpl.java.vm");
        templates.add("templates/Controller.java.vm");
        templates.add("templates/RemoteService.java.vm");
        templates.add("templates/RemoteServiceFallBackImpl.java.vm");
        templates.add("templates/RemoteServiceFallBackFactory.java.vm");
        return templates;
    }

    private static String getControllerRootPath(String tableName) {
        return tableName.replaceAll(Constants.UNDERSCORE, Constants.SLASH);
    }

    private static String getFileName(GenerateAbility generateAbility, String template, String className,
        String packageName, String moduleName) {
        String packagePath = ToolConstants.DEFAULT_BACK_END_PROJECT + File.separator + "src" + File.separator + "main"
            + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
        }

        if (generateAbility.isEntity()) {
            if (template.contains(TEMPLATES + ENTITY_JAVA_VM)) {
                return packagePath + "entity" + File.separator + className + "DO.java";
            }
        }

        if (generateAbility.isVo()) {
            if (template.contains(TEMPLATES + VO_JAVA_VM)) {
                return packagePath + "vo" + File.separator + className + "VO.java";
            }
        }

        if (generateAbility.isDto()) {
            if (template.contains(TEMPLATES + DTO_JAVA_VM)) {
                return packagePath + "dto" + File.separator + className + "DTO.java";
            }
        }

        if (generateAbility.isEntityColumn()) {
            if (template.contains(TEMPLATES + ENTITY_COLUMN_JAVA_VM)) {
                return packagePath + "entity" + File.separator + "column" + File.separator + className + "Column.java";
            }
        }

        if (generateAbility.isMapper()) {
            if (template.contains(TEMPLATES + MAPPER_JAVA_VM)) {
                return packagePath + "mapper" + File.separator + className + "Mapper.java";
            }
        }

        if (generateAbility.isService()) {
            if (template.contains(TEMPLATES + SERVICE_JAVA_VM)) {
                return packagePath + "service" + File.separator + "I" + className + "Service.java";
            }
        }

        if (generateAbility.isServiceImpl()) {
            if (template.contains(TEMPLATES + SERVICE_IMPL_JAVA_VM)) {
                return packagePath + "service" + File.separator + "impl" + File.separator + className
                    + "ServiceImpl.java";
            }
        }

        if (generateAbility.isController()) {
            if (template.contains(TEMPLATES + CONTROLLER_JAVA_VM)) {
                return packagePath + "controller" + File.separator + className + "Controller.java";
            }
        }

        if (generateAbility.isRemoteService()) {
            if (template.contains(TEMPLATES + REMOTE_SERVICE_JAVA_VM)) {
                return packagePath + "feign" + File.separator + "IRemote" + className + "Service.java";
            }
        }

        if (generateAbility.isRemoteServiceFallBackImpl()) {
            if (template.contains(TEMPLATES + REMOTE_SERVICE_FALLBACK_IMPL_JAVA_VM)) {
                return packagePath + "feign" + File.separator + "fallback" + File.separator + "Remote" + className
                    + "ServiceFallbackImpl.java";
            }
        }

        if (generateAbility.isRemoteServiceFallBackFactory()) {
            if (template.contains(TEMPLATES + REMOTE_SERVICE_FALLBACK_FACTORY_JAVA_VM)) {
                return packagePath + "feign" + File.separator + "factory" + File.separator + "Remote" + className
                    + "ServiceFallbackFactory.java";
            }
        }

        if (generateAbility.isMapperXml()) {
            if (template.contains(TEMPLATES + MAPPER_XML_VM)) {
                return ToolConstants.DEFAULT_BACK_END_PROJECT + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources" + File.separator + "mapper" + File.separator + className
                    + "Mapper.xml";
            }
        }

        return null;
    }

}
