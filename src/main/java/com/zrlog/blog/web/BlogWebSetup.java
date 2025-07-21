package com.zrlog.blog.web;

import com.hibegin.common.util.IOUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.server.api.Interceptor;
import com.zrlog.blog.web.config.BlogRouters;
import com.zrlog.blog.web.config.ZrLogHttpRequestListener;
import com.zrlog.blog.web.interceptor.BlogApiInterceptor;
import com.zrlog.blog.web.interceptor.BlogPageInterceptor;
import com.zrlog.blog.web.interceptor.BlogPluginInterceptor;
import com.zrlog.blog.web.interceptor.PwaInterceptor;
import com.zrlog.blog.web.plugin.ArticleStatisticsPluginImpl;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.blog.web.plugin.TemplateDownloadPlugin;
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
            String[] resources = IOUtil.getStringInputStream(BlogWebSetup.class.getResourceAsStream("/resource.txt")).split("\n");
            for (String resource : resources) {
                if (StringUtils.isEmpty(resource)) {
                    continue;
                }
                IOUtil.getByteByInputStream(BlogWebSetup.class.getResourceAsStream(resource));
            }
        }
        zrLogConfig.getServerConfig().addRequestListener(new ZrLogHttpRequestListener());
    }

    @Override
    public void setup() {
        List<Class<? extends Interceptor>> interceptors = zrLogConfig.getServerConfig().getInterceptors();
        interceptors.add(PwaInterceptor.class);
        interceptors.add(BlogPluginInterceptor.class);
        interceptors.add(BlogApiInterceptor.class);
        interceptors.add(BlogPageInterceptor.class);
        //
        BlogRouters.configBlogRouter(zrLogConfig.getServerConfig().getRouter());
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
