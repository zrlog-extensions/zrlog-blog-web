package com.zrlog.blog.web;

import com.google.gson.Gson;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.util.BeanUtil;
import com.hibegin.common.util.IOUtil;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.ApplicationContext;
import com.hibegin.http.server.api.Interceptor;
import com.hibegin.http.server.util.FreeMarkerUtil;
import com.hibegin.http.server.util.HttpRequestBuilder;
import com.hibegin.http.server.util.NativeImageUtils;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.web.config.BlogRouters;
import com.zrlog.blog.web.config.ZrLogHttpRequestListener;
import com.zrlog.blog.web.interceptor.BlogApiInterceptor;
import com.zrlog.blog.web.interceptor.BlogPageInterceptor;
import com.zrlog.blog.web.interceptor.BlogPluginInterceptor;
import com.zrlog.blog.web.interceptor.PwaInterceptor;
import com.zrlog.blog.web.plugin.ArticleStatisticsPluginImpl;
import com.zrlog.blog.web.plugin.BlogPageStaticSitePlugin;
import com.zrlog.blog.web.plugin.TemplateDownloadPlugin;
import com.zrlog.blog.web.util.Outline;
import com.zrlog.blog.web.util.PagerUtil;
import com.zrlog.blog.web.util.PagerVO;
import com.zrlog.common.Constants;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.common.vo.I18nVO;
import com.zrlog.common.vo.Version;
import com.zrlog.data.cache.vo.Archive;
import com.zrlog.data.cache.vo.BaseDataInitVO;
import com.zrlog.data.cache.vo.HotLogBasicInfoEntry;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSetup;
import org.apache.commons.dbutils.BasicRowProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlogWebSetup implements WebSetup {

    private final ZrLogConfig zrLogConfig;
    private final String contextPath;

    private void nativeImage() {
        String[] resources = IOUtil.getStringInputStream(BlogWebSetup.class.getResourceAsStream("/resource.txt")).split("\n");
        NativeImageUtils.doResourceLoadByResourceNames(Arrays.stream(resources).filter(StringUtils::isNotEmpty).map(e -> "/" + e).collect(Collectors.toList()));

        try {
            FreeMarkerUtil.init(PathUtil.getStaticFile(Constants.DEFAULT_TEMPLATE_PATH).getPath());
        } catch (Exception e) {
            LoggerUtil.getLogger(BlogWebSetup.class).info("Freemarker init error " + e.getMessage());
        }
        try {
            FreeMarkerUtil.initClassTemplate(Constants.DEFAULT_TEMPLATE_PATH);
        } catch (Exception e) {
            LoggerUtil.getLogger(BlogWebSetup.class).info("Freemarker init error " + e.getMessage());
        }
        try {
            ApplicationContext applicationContext = new ApplicationContext(zrLogConfig.getServerConfig());
            applicationContext.init();
            FreeMarkerUtil.renderToFM("empty", HttpRequestBuilder.buildRequest(HttpMethod.GET, "/", "",
                    "", zrLogConfig.getRequestConfig(),
                    applicationContext));
        } catch (Exception e) {
            LoggerUtil.getLogger(BlogWebSetup.class).info("Freemarker render error " + e.getMessage());
        }
    }

    public static class MyBasicRowProcessor extends BasicRowProcessor {
        public static Map<String, Object> createMap() {
            return createCaseInsensitiveHashMap(2);
        }
    }

    private static void nativeJson(){
        new Gson().toJson(new HotLogBasicInfoEntry());
        new Gson().toJson(new BaseDataInitVO());
        new Gson().toJson(new PageData<>());
        new Gson().toJson(MyBasicRowProcessor.createMap());
        ArrayList<Object> objects = new ArrayList<>();
        objects.add(MyBasicRowProcessor.createMap());
        new Gson().toJson(new PageData<>(0L, objects));
        new Gson().toJson(new PageData<>(0L, new ArrayList<>(), 0L, 0L));
        new Gson().toJson(new BaseDataInitVO.Statistics());
        new Gson().toJson(new I18nVO());
        new Gson().toJson(new Outline());
        new Gson().toJson(new Version());
        new Gson().toJson(PagerUtil.generatorPager("/all", 1, 20));
        new Gson().toJson(new PagerVO.PageEntry());
        new Gson().toJson(new Archive());
        BeanUtil.cloneObject(MyBasicRowProcessor.createMap());
    }

    public BlogWebSetup(ZrLogConfig zrLogConfig, String contextPath) {
        this.zrLogConfig = zrLogConfig;
        this.contextPath = contextPath;
        if (zrLogConfig.getServerConfig().isNativeImageAgent()) {
            nativeImage();
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
