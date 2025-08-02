package com.zrlog.blog.web.util;

import com.google.gson.Gson;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.util.BeanUtil;
import com.hibegin.common.util.IOUtil;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.ApplicationContext;
import com.hibegin.http.server.util.FreeMarkerUtil;
import com.hibegin.http.server.util.HttpRequestBuilder;
import com.hibegin.http.server.util.NativeImageUtils;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.web.BlogWebSetup;
import com.zrlog.blog.web.template.vo.ArticleDetailPageVO;
import com.zrlog.blog.web.template.vo.ArticleListPageVO;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.common.Constants;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BlogNativeImageUtils {

    public static void reg(ZrLogConfig zrLogConfig) {
        nativeJson();
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


    public static void nativeJson() {
        //freemarker need
        regWithGetMethod(ArticleDetailPageVO.class, ArticleListPageVO.class,
                BasePageInfo.class, ArticleBasicDTO.class, ArticleDetailDTO.class,
                ArticleDetailDTO.LastLogDTO.class, ArticleDetailDTO.NextLogDTO.class,
                ArticleDetailDTO.TagsDTO.class, PagerVO.PageEntry.class, PagerVO.class, PagerVO.PageEntry.class);
    }

    private static void regWithGetMethod(Class<?>... objects) {
        NativeImageUtils.gsonNativeAgentByClazz(List.of(objects));
        for (Class<?> o : objects) {
            NativeImageUtils.regGetMethodByClassName(o);
        }
    }
}
