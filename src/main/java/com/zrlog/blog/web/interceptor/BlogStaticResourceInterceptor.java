package com.zrlog.blog.web.interceptor;

import com.hibegin.http.server.api.HandleAbleInterceptor;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.common.Constants;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.util.StaticFileCacheUtils;
import com.zrlog.util.ZrLogUtil;

import java.io.File;
import java.util.Objects;

public class BlogStaticResourceInterceptor implements HandleAbleInterceptor {


    public BlogStaticResourceInterceptor() {
    }

    @Override
    public boolean doInterceptor(HttpRequest request, HttpResponse response) {
        File staticFile = PathUtil.getStaticFile(request.getUri());
        //静态文件进行拦截
        if (staticFile.isFile() && staticFile.exists()) {
            //缓存静态资源文件
            if (StaticFileCacheUtils.getInstance().isCacheableByRequest(request.getUri())) {
                ZrLogUtil.putLongTimeCache(response);
            }
            response.writeFile(staticFile);
            return false;
        }
        BlogPageStaticSitePlugin staticSitePlugin = Constants.zrLogConfig.getPlugin(BlogPageStaticSitePlugin.class);
        if (Objects.nonNull(staticSitePlugin)) {
            File cacheFile = staticSitePlugin.loadCacheFile(request);
            if (cacheFile.isFile() && cacheFile.exists()) {
                response.writeFile(cacheFile);
                return false;
            }
        }
        return true;
    }

    /**
     * staticPlugin，自己控制缓存文件的读取和生成方式
     *
     * @param request
     * @return
     */
    @Override
    public boolean isHandleAble(HttpRequest request) {
        if (request.getUri().startsWith("/admin")) {
            return false;
        }
        return !BaseStaticSitePlugin.isStaticPluginRequest(request);
    }
}
