package com.zrlog.blog.web;

import com.hibegin.http.server.api.Interceptor;
import com.zrlog.blog.web.config.BlogRouters;
import com.zrlog.blog.web.config.ZrLogRequestRecordListener;
import com.zrlog.blog.web.interceptor.BlogApiInterceptor;
import com.zrlog.blog.web.interceptor.BlogPageInterceptor;
import com.zrlog.blog.web.interceptor.BlogPluginInterceptor;
import com.zrlog.blog.web.interceptor.BlogStaticResourceInterceptor;
import com.zrlog.blog.web.plugin.ArticleStatisticsPluginImpl;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.blog.web.plugin.TemplateDownloadPlugin;
import com.zrlog.blog.web.util.BlogNativeImageUtils;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSetup;

import java.util.List;

public class BlogWebSetup implements WebSetup {

    private final ZrLogConfig zrLogConfig;
    private final String contextPath;


    public BlogWebSetup(ZrLogConfig zrLogConfig, String contextPath) {
        this.zrLogConfig = zrLogConfig;
        this.contextPath = contextPath;
        if (zrLogConfig.getServerConfig().isNativeImageAgent()) {
            BlogNativeImageUtils.reg(zrLogConfig);
        }
        zrLogConfig.getServerConfig().addFirstRequestListener(new ZrLogRequestRecordListener());
    }

    @Override
    public void setup() {
        List<Class<? extends Interceptor>> interceptors = zrLogConfig.getServerConfig().getInterceptors();
        interceptors.add(BlogPluginInterceptor.class);
        interceptors.add(BlogStaticResourceInterceptor.class);
        interceptors.add(BlogApiInterceptor.class);
        interceptors.add(BlogPageInterceptor.class);
        //
        BlogRouters.configBlogRouter(zrLogConfig.getServerConfig().getRouter());
        zrLogConfig.getServerConfig().addStaticResourceMapper("/assets/css", "/assets/css", BlogWebSetup.class::getResourceAsStream);
        zrLogConfig.getServerConfig().addStaticResourceMapper("/assets/js", "/assets/js", BlogWebSetup.class::getResourceAsStream);
    }

    @Override
    public Plugins getPlugins() {
        Plugins plugins = new Plugins();
        plugins.add(new TemplateDownloadPlugin());
        plugins.add(new BlogPageStaticSitePlugin(zrLogConfig, contextPath));
        plugins.add(new ArticleStatisticsPluginImpl());
        return plugins;
    }
}
