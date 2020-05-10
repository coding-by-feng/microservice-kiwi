import me.fengorz.kiwi.common.sdk.bean.EnhancedBeanUtils;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019-09-10 14:19
 */
public class GeneratorTest {
    public static void main(String[] args) {
        // Map<String, String> map = new HashMap<>();
        // map.put("tableName", "test");
        // ObjectMapper objectMapper = new ObjectMapper();
        // TableEntity tableEntity = objectMapper.convertValue(map, TableEntity.class);
        // Map map1 = objectMapper.convertValue(tableEntity, Map.class);
        // map1.forEach((k, v) -> {
        //     System.out.println(k + "+" + v);
        // });

        System.out.println(EnhancedBeanUtils.columnToBeanProperty("t_tactic_scheme", "_"));
    }
}
