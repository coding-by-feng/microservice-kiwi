import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import me.fengorz.kason.generator.CustomCodeGenerator;
import me.fengorz.kason.generator.config.MybatisPlusConfigurer;
import me.fengorz.kason.generator.entity.GenerateAbility;
import me.fengorz.kason.generator.entity.GenerateConfig;
import me.fengorz.kason.generator.service.SysGeneratorService;

/**
 * @Author Kason Zhan @Date 2019-09-16 16:33
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MybatisPlusConfigurer.class)
public class CustomCodeGeneratorTest {

    @Autowired
    private SysGeneratorService sysGeneratorService;

    @Test
    public void test() throws Exception {
        String tableName = "word_review_daily_counter";
        GenerateConfig config = new GenerateConfig();
        // "请输入表明前缀，比如t_table_name的话输入\"t_\""
        // 每次这里编译报错都要先在pom.xml加上：
        /**
         * <plugin> <groupId>org.springframework.boot</groupId> <artifactId>spring-boot-maven-plugin</artifactId>
         * </plugin>
         */
        // 然后再删掉就可以，为什么？
        config.setTablePreName("");
        // 请输入模块名称：
        config.setModuleName("biz");
        config.setPackageName("me.fengorz.kason.word");
        // 请输入@FeignClient的value属性值,不需要的话置空
        config.setServiceId(null);
        // 请生成代码的zip压缩包要存储的本地Path(比如：/Users/zhanshifeng/Documents/myDocument/temp/20200224/generator.zip)：
        config.setZipPath("/Users/zhanshifeng/Documents/myDocument/temp/20210819/generator.zip");

        GenerateAbility ability = new GenerateAbility();
        // 开启Controller自动生成
        ability.setController(true);
        ability.setEntity(true);
        ability.setService(true);
        ability.setServiceImpl(true);
        ability.setMapper(true);
        ability.setMapperXml(true);
        ability.setVo(true);
        ability.setDto(true);
        ability.setRemoteService(true);
        ability.setRemoteServiceFallBackImpl(true);
        ability.setRemoteServiceFallBackFactory(true);

        Map<String, String> table = sysGeneratorService.queryTable(tableName);
        List<Map<String, String>> columns = sysGeneratorService.queryColumns(tableName);
        CustomCodeGenerator.generatorCode(ability, config, table, columns);
    }

    private Configuration getConfig() throws ConfigurationException {
        return new PropertiesConfiguration("generator.properties");
    }
}
