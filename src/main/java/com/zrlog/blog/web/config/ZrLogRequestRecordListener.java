package com.zrlog.blog.web.config;

import com.hibegin.common.util.LoggerUtil;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpRequestListener;
import com.hibegin.http.server.api.HttpResponse;
import com.zrlog.blog.web.util.WebTools;
import com.zrlog.business.plugin.ArticleStatisticsPlugin;
import com.zrlog.business.plugin.RequestInfo;
import com.zrlog.common.Constants;
import com.zrlog.data.dto.ArticleDetailDTO;

import java.util.Objects;
import java.util.logging.Logger;

public class ZrLogRequestRecordListener implements HttpRequestListener {

    private static final Logger LOGGER = LoggerUtil.getLogger(HttpRequestListener.class);

    /**
     * 判断请求是否为文章详情页
     *
     * @param request
     * @return
     */
    private boolean isArticlePage(HttpRequest request) {
        if (!Constants.zrLogConfig.isInstalled()) {
            return false;
        }
        if (request.getMethod() != HttpMethod.GET) {
            return false;
        }
        String actionKey = request.getUri();
        //仅保留非静态资源请求或者是以 .html结尾的
        if (actionKey.contains(".") && !actionKey.endsWith(".html")) {
            return false;
        }
        return Objects.nonNull(request.getAttr().get("log"));
    }


    @Override
    public void onHandled(HttpRequest request, HttpResponse httpResponse) {
        long used = System.currentTimeMillis() - request.getCreateTime();
        if (isArticlePage(request)) {
            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setIp(WebTools.getRealIp(request));
            requestInfo.setUrl(WebTools.getHomeUrl(request));
            requestInfo.setUserAgent(request.getHeader("User-Agent"));
            requestInfo.setRequestTime(request.getCreateTime());
            requestInfo.setRequestUri(request.getUri());
            ArticleDetailDTO log = (ArticleDetailDTO) request.getAttr().get("log");
            requestInfo.setArticleId(log.getLogId());
            requestInfo.setUsedTime(used);
            ArticleStatisticsPlugin plugin = Constants.zrLogConfig.getPlugin(ArticleStatisticsPlugin.class);
            if (Objects.nonNull(plugin)) {
                plugin.record(requestInfo);
            }
        }
    }
}
