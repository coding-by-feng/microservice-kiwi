
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
import org.junit.jupiter.api.Test;

public class JasyptTests {

    @Test
    public void main() {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        String password = System.getenv("KIWI_ENC_PASSWORD");
        String plainText = "11111111";
        textEncryptor.setPassword(password);
        String cipherText = textEncryptor.encrypt(plainText);
        String decrypt = textEncryptor.decrypt("LnyjqsOsAhLH6oOlKozLFg==");
        System.out.println(cipherText);
        System.out.println(decrypt);
        System.out.println(textEncryptor.decrypt("PvjdIGuFWiYPJ4VxpWK+nAeW/V+Je+xV"));
    }
}

