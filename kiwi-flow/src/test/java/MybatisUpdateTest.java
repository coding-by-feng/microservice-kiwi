/*
 *
 *   Copyright [2019~2025] [codingByFeng]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *
 */

import org.apache.ibatis.session.SqlSession;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description TODO
 * @Author zhanshifeng
 * @Date 2019/12/12 9:19 AM
 */
public class MybatisUpdateTest {

    public static void main(String[] args) {
        long thisTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            SqlSession sqlSession = SqlSessionFactoryUtil.getSqlSession();
            Map map = new HashMap();
            map.put("tableName", "word_main");
            // sqlSession.selectOne("me.fengorz.kiwi.generator.mapper.SysGeneratorMapper.queryTable", map);
            long tempTime = System.currentTimeMillis();
            try {
                sqlSession.update("me.fengorz.kiwi.generator.mapper.SysGeneratorMapper.testUpdate");
            } catch (RuntimeException e) {
                long waitTime = (tempTime - thisTime) / 1000L;
                thisTime = tempTime;
                System.out.println("Locking wait for" + waitTime + "s");
                continue;
            }
            sqlSession.commit();
            sqlSession.close();
            System.out.println("openSqlSession" + i);
        }
    }

}
