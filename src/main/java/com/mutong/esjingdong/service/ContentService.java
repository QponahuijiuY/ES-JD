package com.mutong.esjingdong.service;

import com.mutong.esjingdong.bean.Content;
import com.mutong.esjingdong.utils.HtmlParseUtil;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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


    public Boolean parseContent(String keywords){
        List<Content> contents = new HtmlParseUtil().parseJD(keywords);
    }
}
