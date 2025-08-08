package com.zrlog.blog.web.interceptor;

import com.hibegin.http.server.api.HandleAbleInterceptor;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.web.template.ZrLogTemplateRender;
import com.zrlog.common.Constants;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 静态化文章页，加快文章页的响应，压缩html文本，提供自定义插件标签的解析，静态资源文件的浏览器缓存问题
 */
public class BlogPageInterceptor implements HandleAbleInterceptor {


    @Override
    public boolean doInterceptor(HttpRequest request, HttpResponse response) throws Exception {
        String target = request.getUri();
        if (!Constants.zrLogConfig.isInstalled()) {
            response.redirect("/install?ref=" + request.getUri());
            return false;
        }
        Method method = request.getServerConfig().getRouter().getRouterMap().get(target);
        if (Objects.isNull(method) && target.endsWith(".html")) {
            method = request.getServerConfig().getRouter().getRouterMap().get(target.substring(0, target.length() - 5));
        }
        if (target.startsWith("/all-")) {
            method = request.getServerConfig().getRouter().getRouterMap().get("/index");
        }
        if (target.startsWith("/sort/")) {
            method = request.getServerConfig().getRouter().getRouterMap().get("/sort");
        }
        if (target.startsWith("/search/")) {
            method = request.getServerConfig().getRouter().getRouterMap().get("/search");
        }
        if (target.startsWith("/tag/")) {
            method = request.getServerConfig().getRouter().getRouterMap().get("/tag");
        }
        if (target.startsWith("/record/")) {
            method = request.getServerConfig().getRouter().getRouterMap().get("/record");
        }
        if (Objects.isNull(method)) {
            method = request.getServerConfig().getRouter().getRouterMap().get("/detail");
        }
        if (Objects.isNull(method)) {
            response.renderCode(404);
            return false;
        }
        Object invoke = method.invoke(Controller.buildController(method, request, response));
        if (Objects.nonNull(invoke)) {
            String htmlStr = new ZrLogTemplateRender(request).renderByTemplateName(invoke.toString());
            response.renderHtmlStr(htmlStr);
        }
        return false;
    }

    @Override
    public boolean isHandleAble(HttpRequest request) {
        String uri = request.getUri();
        return !Objects.equals(uri, "/install") && !uri.startsWith("/install/");
    }
}
