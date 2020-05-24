import cn.hutool.core.lang.Assert;
import lombok.SneakyThrows;
import me.fengorz.kiwi.word.api.dto.fetch.FetchParaphraseDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchParaphraseExampleDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordCodeDTO;
import me.fengorz.kiwi.word.api.dto.fetch.FetchWordResultDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// import me.fengorz.kiwi.common.core.util.JsonPackagedUtil;

/**
 * @Description TODO
 * @Author codingByFeng
 * @Date 2019/10/23 9:17 PM
 */
public class JsoupTest {

    @SneakyThrows
    public static void main(String[] args) {
        final String word = "reveal" ;
        final String url = "https://dictionary.cambridge.org/zhs/词典/英语-汉语-简体/" ;
        Document doc = Jsoup.connect(url + word).get();
        Elements root = doc.getElementsByClass("pr entry-body__el");
        Assert.notNull(root, "The {} is not found!" , word);

        FetchWordResultDTO fetchWordResultDTO = new FetchWordResultDTO();
        fetchWordResultDTO.setWordName(word);
        List<FetchWordCodeDTO> fetchWordCodeDTOList = new ArrayList<>();

        root.forEach(block -> {
            Elements mainParaphrases = block.getElementsByClass("sense-body dsense_b");
            Assert.notNull(mainParaphrases, "The mainParaphrases of {} is not found!" , word);
            FetchWordCodeDTO fetchWordCodeDTO = new FetchWordCodeDTO();
            List<FetchParaphraseDTO> fetchParaphraseDTOList = new ArrayList<>();

            Optional.ofNullable(block.getElementsByClass("pos-header dpos-h")).filter(elements -> elements.size() == 1).ifPresent(elements -> {
                elements.forEach(head -> {
                    Optional.ofNullable(head.getElementsByClass("pos dpos")).flatMap(element -> Optional.ofNullable(element.text())).ifPresent(code -> {
                        fetchWordCodeDTO.setCode(code);
                    });
                    Optional.ofNullable(head.getElementsByClass("gram dgram")).flatMap(element -> Optional.ofNullable(element.text())).ifPresent(label -> {
                        fetchWordCodeDTO.setLabel(label);
                    });
                });
            });

            mainParaphrases.forEach(paraphrases -> {
                Elements singleParaphrase = paraphrases.getElementsByClass("def-block ddef_block ");
                Assert.notNull(singleParaphrase, "The singleParaphrase of {} is not found!" , word);

                singleParaphrase.forEach(paraphrase -> {
                    FetchParaphraseDTO fetchParaphraseDTO = new FetchParaphraseDTO();

                    Elements paraphraseEnglish = paraphrase.getElementsByClass("def ddef_d");
                    Assert.notNull(paraphraseEnglish, "The paraphraseEnglish of {} is not found!" , word);
                    fetchParaphraseDTO.setParaphraseEnglish(paraphraseEnglish.text());

                    // fetch chinese meaning
                    Elements meaningChinese = paraphrase.getElementsByClass("trans dtrans dtrans-se ");
                    Assert.notNull(meaningChinese, "The meaningChinese of {} is not found!" , word);
                    fetchParaphraseDTO.setMeaningChinese(meaningChinese.text());

                    // fetch example sentence
                    Elements exampleSentences = paraphrase.getElementsByClass("examp dexamp");
                    if (!exampleSentences.isEmpty()) {
                        List<FetchParaphraseExampleDTO> fetchParaphraseExampleDTOList = new ArrayList<>();
                        exampleSentences.forEach(sentence -> {
                            FetchParaphraseExampleDTO fetchParaphraseExampleDTO = new FetchParaphraseExampleDTO();
                            Optional.ofNullable(sentence.getElementsByClass("eg deg")).ifPresent(
                                    elements -> {
                                        fetchParaphraseExampleDTO.setExampleSentence(elements.text());
                                    }
                            );
                            Optional.ofNullable(sentence.getElementsByClass("trans dtrans dtrans-se hdb")).ifPresent(
                                    elements -> {
                                        fetchParaphraseExampleDTO.setExampleTranslate(elements.text());
                                    }
                            );

                            // TODO codingByFeng The default is English, but consider how flexible it will be in the future if there are other languages
                            fetchParaphraseExampleDTO.setTranslateLanguage("English");
                            fetchParaphraseExampleDTOList.add(fetchParaphraseExampleDTO);
                            fetchParaphraseDTO.setFetchParaphraseExampleDTOList(fetchParaphraseExampleDTOList);
                        });
                    }

                    fetchParaphraseDTOList.add(fetchParaphraseDTO);
                });
            });

            fetchWordCodeDTO.setFetchParaphraseDTOList(fetchParaphraseDTOList);
            fetchWordCodeDTOList.add(fetchWordCodeDTO);
        });

        fetchWordResultDTO.setFetchWordCodeDTOList(fetchWordCodeDTOList);

        // System.out.println(JsonPackagedUtil.toJsonStr(wordDTO));
    }

    // private Stream<String> getParaphrasePerBlock(Element) {
    //     return null;
    // }
}
