
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

import org.jasypt.util.text.BasicTextEncryptor;

public class JasyptTests {

    public static void main(String[] args) {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        String password = "coding-by-feng";
        String plainText = "fengORZ123";
        textEncryptor.setPassword(password);
        String cipherText = textEncryptor.encrypt(plainText);
        String decrypt = textEncryptor.decrypt("uxuSI1Bl5AelC8dYvJUvMhsRbasVD4zh");
        System.out.println(cipherText);
        System.out.println(decrypt);
    }
}

