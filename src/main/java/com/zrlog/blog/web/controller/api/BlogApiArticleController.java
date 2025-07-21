package com.zrlog.blog.web.controller.api;

import com.hibegin.common.dao.dto.PageData;
import com.hibegin.http.annotation.RequestMethod;
import com.hibegin.http.annotation.ResponseBody;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.rest.response.ApiStandardResponse;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.business.util.ControllerUtil;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;

import java.sql.SQLException;

public class BlogApiArticleController extends Controller {

    private final ArticleService articleService = new ArticleService();

    @ResponseBody
    @RequestMethod
    public ApiStandardResponse<ArticleDetailDTO> detail() throws SQLException {
        return new ApiStandardResponse<>(articleService.detail(request.getParaToStr("id", ""), request));
    }

    @ResponseBody
    @RequestMethod
    public ApiStandardResponse<PageData<ArticleBasicDTO>> index() {
        String key = getRequest().getParaToStr("key", "");
        return new ApiStandardResponse<>(articleService.pageByKeywords(ControllerUtil.getPageRequest(this), key, getRequest()));
    }
}
