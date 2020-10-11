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

import com.squareup.okhttp.*;
import okio.BufferedSink;
import okio.Okio;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @Author zhanshifeng
 * @Date 2020/10/8 6:12 PM
 */
public class RapidAPITEst {

    @Test
    public void test() {
        OkHttpClient client = new OkHttpClient();

        // Request request = new Request.Builder()
        //         .url("https://voicerss-text-to-speech.p.rapidapi.com/?r=0&c=mp3&f=8khz_8bit_mono&src=Hello%252C%20world!&hl=en-us&c=mp3")
        //         .get()
        //         .addHeader("x-rapidapi-host", "voicerss-text-to-speech.p.rapidapi.com")
        //         .addHeader("x-rapidapi-key", "d9db126fa8msh119775dd97648d1p1390cejsn06b45bdd9499")
        //         .build();


        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "c=mp3&r=0&f=8khz_8bit_mono&src=Hello%2C%20world!&hl=en-us");
        Request request = new Request.Builder()
                .url("https://voicerss-text-to-speech.p.rapidapi.com/")
                .post(body)
                .addHeader("x-rapidapi-host", "voicerss-text-to-speech.p.rapidapi.com")
                .addHeader("x-rapidapi-key", "d2d13085141645ac87b12729dbb7ebe6")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        try {
            Response response = client.newCall(request).execute();
            // System.out.println(System.getProperty("user.dir"));
            // System.out.println(System.getProperty("java.class.path"));
            File downloadedFile = new File("/Users/zhanshifeng/Documents/myDocument/temp/20201008", "test");
            BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
            sink.writeAll(response.body().source());
            sink.close();
            System.out.println("success!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
