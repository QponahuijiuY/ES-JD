package com.mutong.esjingdong.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @description: 获取请求返回的页面信息,筛选
 * @Author: Mutong
 * @Date: 2020-04-06 22:26
 * @time_complexity: O()
 */
@Controller
public class IndexController {
    @GetMapping({"/" , "/index"})
    public String getIndex(){
        return "index";
    }
}
