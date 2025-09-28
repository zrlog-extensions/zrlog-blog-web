package com.zrlog.blog.web.controller.api;

import com.hibegin.http.annotation.RequestMethod;
import com.hibegin.http.annotation.ResponseBody;
import com.hibegin.http.server.web.Controller;
import com.zrlog.common.Constants;
import com.zrlog.blog.business.rest.response.ApiStandardResponse;
import com.zrlog.common.cache.vo.BaseDataInitVO;

public class BlogApiCacheController extends Controller {

    @ResponseBody
    @RequestMethod
    public ApiStandardResponse<BaseDataInitVO> index() {
        return new ApiStandardResponse<>(Constants.zrLogConfig.getCacheService().getInitData());
    }
}
