package com.mutong.esjingdong.service;

import com.alibaba.fastjson.JSON;
import com.mutong.esjingdong.bean.Content;
import com.mutong.esjingdong.utils.HtmlParseUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @Author: Mutong
 * @Date: 2020-04-06 22:48
 * @time_complexity: O()
 */
@Service
public class ContentService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;


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

    //获取这些数据
    public List<Map<String,Object>> searchPage(String keyword, int pageNo, int pageSize) throws IOException {
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

        //执行搜索
        ArrayList<Map<String,Object>> list = new ArrayList<>();
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
            list.add(documentFields.getSourceAsMap());
        }
        return list;
    }


    //搜索高亮
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

}
