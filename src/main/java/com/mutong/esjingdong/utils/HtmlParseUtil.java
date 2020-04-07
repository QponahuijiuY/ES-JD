package com.mutong.esjingdong.utils;

import com.mutong.esjingdong.bean.Content;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @Author: Mutong
 * @Date: 2020-04-06 22:30
 * @time_complexity: O()
 */
@Component
public class HtmlParseUtil {
    public static void main(String[] args) throws IOException {
        new HtmlParseUtil().parseJD("vue").forEach(System.out::println);

    }
    public List<Content> parseJD(String keyword) throws IOException {
        //获取请求
        String url = "https://search.jd.com/Search?keyword="+keyword;
        //解析网页,jsoup返回的就是浏览器的document对象
        Document document = Jsoup.parse(new URL(url), 30000);
        //所有你在js中使用的方法,你都可以使用
        Element element = document.getElementById("J_goodsList");
        //找到所有的li元素
        Elements elements = element.getElementsByTag("li");
        List<Content> goodsList = new ArrayList<Content>();
        //e1  就是li标签里面的内容
        for (Element e1: elements) {
            //关于处理图片,特别多的网站,所有的图片都是延迟加载的,懒加载
            //
            String img = e1.getElementsByTag("img").eq(0).attr("source-data-lazy-img");
            String price = e1.getElementsByClass("p-price").eq(0).text();
            String title = e1.getElementsByClass("p-name").eq(0).text();
            Content content = new Content();
            content.setImg(img);
            content.setPrice(price);
            content.setTitle(title);
            goodsList.add(content);
        }
        return goodsList;

    }
}
