package com.zrlog.blog.web.plugin;

import com.hibegin.common.BaseLockObject;
import com.hibegin.common.dao.ResultValueConvertUtils;
import com.hibegin.common.util.*;
import com.hibegin.http.server.ApplicationContext;
import com.hibegin.http.server.config.AbstractServerConfig;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.business.service.TemplateInfoHelper;
import com.zrlog.common.Constants;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.common.vo.TemplateVO;
import com.zrlog.data.dto.FaviconBase64DTO;
import com.zrlog.model.WebSite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class BlogPageStaticSitePlugin extends BaseLockObject implements StaticSitePlugin {

    private static final Logger LOGGER = LoggerUtil.getLogger(BlogPageStaticSitePlugin.class);
    private final AbstractServerConfig serverConfig;
    private final ApplicationContext applicationContext;
    private final Map<String, HandleState> handleStatusPageMap = new ConcurrentHashMap<>();
    private final String contextPath;
    private final PageService pageService;
    private final String defaultLang;
    private final ReentrantLock parseLock = new ReentrantLock();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final List<File> cacheFiles = new CopyOnWriteArrayList<>();

    public BlogPageStaticSitePlugin(AbstractServerConfig abstractServerConfig, String contextPath) {
        this.applicationContext = new ApplicationContext(abstractServerConfig.getServerConfig());
        this.applicationContext.init();
        this.serverConfig = abstractServerConfig;
        this.contextPath = contextPath;
        this.defaultLang = Constants.DEFAULT_LANGUAGE;
        this.pageService = new PageService(this);
        File cacheFolder = getCacheFile("/");
        if (cacheFolder.exists()) {
            FileUtils.deleteFile(cacheFolder.toString());
        }
    }

    @Override
    public ReentrantLock getParseLock() {
        return parseLock;
    }

    @Override
    public Executor getExecutorService() {
        return executorService;
    }

    private void refreshFavicon() {
        FaviconBase64DTO faviconBase64DTO = new WebSite().faviconBase64DTO();
        faviconHandle(faviconBase64DTO.getFavicon_ico_base64(), "/favicon.ico", ResultValueConvertUtils.toBoolean(faviconBase64DTO.getGenerator_html_status()));
    }


    private void handleRobotsTxt(PublicWebSiteInfo webSite) {
        String robotTxt = webSite.getRobotRuleContent();

        if (StringUtils.isEmpty(robotTxt)) {
            return;
        }
        File robotFile = PathUtil.getStaticFile("robots.txt");
        if (!robotFile.getParentFile().exists()) {
            robotFile.getParentFile().mkdirs();
        }
        IOUtil.writeStrToFile(robotTxt, robotFile);
        if (webSite.getGenerator_html_status()) {
            try {
                saveToCacheFolder(new FileInputStream(robotFile), "/" + robotFile.getName());
            } catch (FileNotFoundException e) {
                LOGGER.warning("save to Cache error " + e.getMessage());
            }
        }
    }

    @Override
    public Map<String, HandleState> getHandleStatusPageMap() {
        return handleStatusPageMap;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getDefaultLang() {
        return defaultLang;
    }

    private void copyCommonAssert() {
        //video.js
        copyResourceToCacheFolder("/assets/css/font/vjs.eot");
        copyResourceToCacheFolder("/assets/css/font/vjs.svg");
        copyResourceToCacheFolder("/assets/css/font/vjs.ttf");
        copyResourceToCacheFolder("/assets/css/font/vjs.woff");
        copyResourceToCacheFolder("/assets/css/video-js.css");
        copyResourceToCacheFolder("/assets/css/katex.min.css");
        copyResourceToCacheFolder("/assets/js/video.js");
        copyResourceToCacheFolder("/assets/css/hljs/dark.css");
        copyResourceToCacheFolder("/assets/css/hljs/light.css");
        //default avatar url
        //copyResourceToCacheFolder("/assets/images/default-portrait.gif");
        File faviconFile = PathUtil.getStaticFile("/favicon.ico");
        if (faviconFile.exists()) {
            try {
                saveToCacheFolder(new FileInputStream(faviconFile), "/" + faviconFile.getName());
            } catch (FileNotFoundException e) {
                LOGGER.warning("Missing resource " + faviconFile);
            }
        } else {
            //favicon
            copyResourceToCacheFolder("/favicon.ico");
        }
    }


    private void copyDefaultTemplateAssets(String templatePath) {
        if (!Objects.equals(templatePath, Constants.DEFAULT_TEMPLATE_PATH)) {
            return;
        }
        TemplateVO templateVO = TemplateInfoHelper.getDefaultTemplateVO();
        templateVO.getStaticResources().forEach(e -> {
            String resourceUri = Constants.DEFAULT_TEMPLATE_PATH + "/" + e;
            copyResourceToCacheFolder(resourceUri);
        });
    }


    @Override
    public String getVersionFileName() {
        return "version.txt";
    }


    @Override
    public String getDbCacheKey() {
        return "static_blog_version";
    }

    private void doGenerator() {
        if (StaticSitePlugin.isDisabled()) {
            return;
        }
        PublicWebSiteInfo webSite = Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo();
        refreshFavicon();
        handleRobotsTxt(webSite);
        copyCommonAssert();
        copyDefaultTemplateAssets(webSite.getTemplate());
        handleStatusPageMap.clear();
        //从首页开始查找
        handleStatusPageMap.put(contextPath + "/", HandleState.NEW);
        //生成 404 页面，用于配置第三方 cdn，或者云存储的错误页面
        handleStatusPageMap.put(notFindFile(), HandleState.NEW);
        pageService.saveRedirectRules(notFindFile());
        doFetch(serverConfig, applicationContext);
    }

    @Override
    public boolean start() {
        cacheFiles.clear();
        lock.lock();
        try {
            doGenerator();
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Override
    public boolean autoStart() {
        if (EnvKit.isFaaSMode()) {
            return false;
        }
        return !StaticSitePlugin.isDisabled();
    }

    @Override
    public boolean isStarted() {
        return lock.isLocked();
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<File> getCacheFiles() {
        return cacheFiles;
    }
}
