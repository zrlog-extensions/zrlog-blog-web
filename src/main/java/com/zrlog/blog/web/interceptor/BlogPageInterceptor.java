package com.zrlog.blog.web.interceptor;

import com.hibegin.common.util.FileUtils;
import com.hibegin.http.server.api.HandleAbleInterceptor;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.util.FreeMarkerUtil;
import com.hibegin.http.server.util.MimeTypeUtil;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.blog.web.plugin.TemplateDownloadPlugin;
import com.zrlog.blog.web.util.TemplateRenderUtils;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.common.Constants;
import com.zrlog.data.cache.vo.BaseDataInitVO;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.util.ZrLogUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 静态化文章页，加快文章页的响应，压缩html文本，提供自定义插件标签的解析，静态资源文件的浏览器缓存问题
 */
public class BlogPageInterceptor implements HandleAbleInterceptor {


    static void setToRequest(HttpRequest servletRequest, BaseDataInitVO cacheInit) {
        if (Objects.isNull(servletRequest)) {
            return;
        }
        if (Objects.isNull(cacheInit)) {
            return;
        }
        servletRequest.getAttr().put("init", cacheInit);
        servletRequest.getAttr().put("website", cacheInit.getWebSite());
        //website alias
        servletRequest.getAttr().put("webSite", cacheInit.getWebSite());
        servletRequest.getAttr().put("webs", cacheInit.getWebSite());
        servletRequest.getAttr().put("WEB_SITE", cacheInit.getWebSite());
    }

    @Override
    public boolean doInterceptor(HttpRequest request, HttpResponse response) throws Exception {
        if (request.getUri().startsWith(Constants.DEFAULT_TEMPLATE_PATH)) {
            try (InputStream resourceAsStream = BlogPageInterceptor.class.getResourceAsStream(request.getUri())) {
                if (Objects.nonNull(resourceAsStream)) {
                    ZrLogUtil.putLongTimeCache(response);
                    response.addHeader("Content-Type", MimeTypeUtil.getMimeStrByExt(FileUtils.getFileExt(request.getUri())));
                    response.write(resourceAsStream, 200);
                    return false;
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        String target = request.getUri();
        if (Constants.zrLogConfig.isInstalled()) {
            setToRequest(request, (BaseDataInitVO) Constants.zrLogConfig.getCacheService().getInitData());
        } else {
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
            TemplateDownloadPlugin templateDownloadPlugin = Constants.zrLogConfig.getPlugin(TemplateDownloadPlugin.class);
            if (Objects.nonNull(templateDownloadPlugin)) {
                templateDownloadPlugin.downloadTemplate(request);
            }
            TemplateRenderUtils.fullTemplateInfo(request);
            TemplateUtils.initTemplate();
            if (TemplateRenderUtils.isArrangeable(request) && TemplateUtils.existsByTemplateName("arrange")) {
                invoke = "arrange";
            }
            String htmlStr = FreeMarkerUtil.renderToFM(invoke.toString(), request);
            render(htmlStr, target, request, response);
        }
        return false;
    }


    private static void render(String htmlStr, String target, HttpRequest request, HttpResponse response) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ResponseRenderPrintWriter responseRenderPrintWriter = new ResponseRenderPrintWriter(byteArrayOutputStream, "/", request, response, null)) {
            responseRenderPrintWriter.write(htmlStr);
            String realHtmlStr = responseRenderPrintWriter.getResponseBody();
            if (!BaseStaticSitePlugin.isStaticPluginRequest(request)) {
                response.renderHtmlStr(realHtmlStr);
            }
            if (Constants.catGeneratorHtml(target) && responseRenderPrintWriter.isRenderSuccess()) {
                BlogPageStaticSitePlugin staticSitePlugin = Constants.zrLogConfig.getPlugin(BlogPageStaticSitePlugin.class);
                if (staticSitePlugin != null) {
                    request.getAttr().put(StaticSitePlugin.STATIC_SITE_PLUGIN_HTML_FILE_KEY, staticSitePlugin.saveResponseBodyToHtml(request, realHtmlStr));
                }
            }
        }
    }

    @Override
    public boolean isHandleAble(HttpRequest request) {
        return true;
    }
}
