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

import java.io.File;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.Data;

/**
 * @Author zhanshifeng @Date 2020/4/22 6:16 PM
 */
public class FundCrawler {

    private Set<Fund> fundCodeSet = new HashSet<>();
    private Set<Fund> greatCodeSet = new HashSet<>();
    private Set<Fund> week1FundCodeSet = new HashSet<>();
    private Set<Fund> month1FundCodeSet = new HashSet<>();
    private Set<Fund> month2FundCodeSet = new HashSet<>();
    private Set<Fund> month3FundCodeSet = new HashSet<>();
    private Set<Fund> month6FundCodeSet = new HashSet<>();
    private Set<Fund> year1FundCodeSet = new HashSet<>();
    private Set<Fund> year2FundCodeSet = new HashSet<>();
    private Set<Fund> year3FundCodeSet = new HashSet<>();
    private Set<Fund> year5FundCodeSet = new HashSet<>();

    public static void main(String[] args) throws Exception {
        FundCrawler fundCrawler = new FundCrawler();
        fundCrawler.run();
    }

    private void run() throws Exception {
        List<String> pathList = new ArrayList<>();
        String week1 = "/Users/zhanshifeng/Documents/myDocument/Document/long-term-bonds/1_week.html";
        String month1 = "/Users/zhanshifeng/Documents/myDocument/Document/long-term-bonds/1_month.html";
        String month3 = "/Users/zhanshifeng/Documents/myDocument/Document/long-term-bonds/3_month.html";
        String month6 = "/Users/zhanshifeng/Documents/myDocument/Document/long-term-bonds/6_month.html";
        String year1 = "/Users/zhanshifeng/Documents/myDocument/Document/long-term-bonds/1_year.html";
        String year2 = "/Users/zhanshifeng/Documents/myDocument/Document/long-term-bonds/2_year.html";
        String year3 = "/Users/zhanshifeng/Documents/myDocument/Document/long-term-bonds/3_year.html";
        String year5 = "/Users/zhanshifeng/Documents/myDocument/Document/long-term-bonds/5_year.html";

        subRun(week1, this.week1FundCodeSet);
        subRun(month1, this.month1FundCodeSet);
        subRun(month3, this.month3FundCodeSet);
        subRun(month6, this.month6FundCodeSet);
        subRun(year1, this.year1FundCodeSet);
        subRun(year2, this.year2FundCodeSet);
        subRun(year3, this.year3FundCodeSet);
        subRun(year5, this.year5FundCodeSet);

        for (Fund fund : this.fundCodeSet) {
            boolean isWeek1 = this.week1FundCodeSet.contains(fund);
            boolean isMonth1 = this.month1FundCodeSet.contains(fund);
            boolean isMonth3 = this.month3FundCodeSet.contains(fund);
            boolean isMonth6 = this.month6FundCodeSet.contains(fund);
            boolean isYear1 = this.year1FundCodeSet.contains(fund);
            boolean isYear2 = this.year2FundCodeSet.contains(fund);
            boolean isYear3 = this.year3FundCodeSet.contains(fund);
            boolean isYear5 = this.year5FundCodeSet.contains(fund);
            // boolean isGreat = isWeek1 && isMonth1 && isMonth3 && isMonth6 && isYear1 && isYear2 &&
            // isYear3 &&
            // isYear5;
            // boolean isGreat = isMonth1 && isMonth3 && isMonth6 && isYear1 && isYear2 && isYear3 &&
            // isYear5;
            boolean isGreat = isWeek1 && isMonth1 && isMonth3 && isMonth6 && isYear1 && isYear2 && isYear3;
            // boolean isGreat = isMonth1 && isMonth3 && isMonth6 && isYear1 && isYear2 && isYear3;
            if (isGreat) {
                this.greatCodeSet.add(fund);
            }
        }

        System.out.println("四四三三法则计算结果：");
        for (Fund greatFund : this.greatCodeSet) {
            System.out.print(greatFund.getCode() + ", ");
        }
    }

    private void subRun(String path, Set<Fund> tempFundCodeSet) throws Exception {

        File in = new File(path);

        Document doc = Jsoup.parse(in, "UTF-8", "");
        Element dbtable = doc.getElementById("dbtable");
        Elements tbody = dbtable.getElementsByTag("tbody");
        Elements hang = tbody.get(0).getElementsByTag("tr");
        for (int x = 0; x < 120; x++) {
            Element element = hang.get(x);
            Elements td = element.select("td");
            for (int i = 0; i < td.size(); i++) {
                String texts = td.get(i).text();
                if (i == 2) {
                    Fund fund = new Fund();
                    fund.setCode(texts);
                    fund.setRanking(x + 1);
                    tempFundCodeSet.add(fund);
                    fundCodeSet.add(fund);
                }
                System.out.print(" " + texts);
            }
            System.out.println();
        }
    }

    @Data
    class Fund {
        String code;
        int ranking;

        @Override
        public String toString() {
            return "Fund{" + "code='" + code + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Fund fund = (Fund)o;
            return code.equals(fund.code);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code);
        }
    }
}
