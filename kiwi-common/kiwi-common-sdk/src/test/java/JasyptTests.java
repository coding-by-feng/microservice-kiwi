
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
        String password = System.getenv("password");
        String plainText = "24.24d4b24a99036efc1f9a341f14309dfb.2592000.1675433329.282335-29512426";
        textEncryptor.setPassword(password);
        String cipherText = textEncryptor.encrypt(plainText);
        String decrypt = textEncryptor.decrypt("5iq0rrNjP/KEC3o61ydTe4uXiA00BXpCCWEZnd5//BjrYwlbeWSajBkKY8q+36N44ehFQdszi7qOnc3TT5647RgxI0zYCqRQzo2HouYZy1E=");
        System.out.println(cipherText);
        System.out.println(decrypt);
    }
}

