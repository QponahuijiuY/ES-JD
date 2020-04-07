# ES-JD
✨ 🏫 ElasticSearch仿京东实现搜索功能,首先根据jsoup包爬取数据,再把数据放在ES里面,之后调用API查询

### 1. 爬虫
##### 1. 导入jsoup依赖
```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.10.2</version>
</dependency>
```
##### 2. 查询
- jsoup 是一款Java 的HTML解析器，可直接解析某个URL地址 所以首先传入京东搜索的URL,
- 解析你想要的标签信息,我们主要是获取每一个商品的题目,价格,以及图片信息,最后遍历返回集合
```java
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

```

### 2. 数据放入ES
- 查询到的数据肯定是批量的,我们需要使用ES的批量添加的API
```java
//把爬虫得到的数据放入es里面
    public Boolean parseContent(String keyword) throws IOException {
        //查询数据
        List<Content> contents = new HtmlParseUtil().parseJD(keyword);
        //把所有的数据放入到es中
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("2m");
        for (int i = 0; i < contents.size(); i++) {
            bulkRequest.add(
                    new IndexRequest("jd_goods")
                    .source(JSON.toJSONString(contents.get(i)), XContentType.JSON)
            );
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return !bulk.hasFailures();
    }
```
### 3. 调用API查询
```java
public List<Map<String,Object>> searchHighlightPage(String keyword, int pageNo, int pageSize) throws IOException {
        if (pageNo<1){
            pageNo = 1;
        }
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //分页
        searchSourceBuilder.from(pageNo);
        searchSourceBuilder.size(pageSize);

        //精准匹配关键字
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword); // 在title字段里面找keyword
        searchSourceBuilder.query(termQueryBuilder);
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        //构建高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //执行搜索
        ArrayList<Map<String,Object>> list = new ArrayList<>();
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
            //解析高亮字段
            //获取高亮字段
            Map<String, HighlightField> highlightFields = documentFields.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();
            //将原来的高亮替换我们之前的
            if (highlightField != null){
                Text[] fragments = highlightField.fragments();
                String title = "";
                for (Text fragment : fragments) {
                    title += fragment;
                }
                sourceAsMap.put("title",title);//替换原来的字段
            }

            list.add(sourceAsMap);
        }
        return list;
    }
```
