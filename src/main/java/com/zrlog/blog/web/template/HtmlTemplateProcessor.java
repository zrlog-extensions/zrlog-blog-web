package com.zrlog.blog.web.template;

import com.hibegin.common.util.IOUtil;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.SecurityUtils;
import com.hibegin.common.util.http.handle.CloseResponseHandle;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.common.Constants;
import com.zrlog.common.vo.AdminTokenVO;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.util.StaticFileCacheUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * 对响应的模板文件生成的 html 进行转化，主要处理静态资源文件和插件相关的逻辑
 */
class HtmlTemplateProcessor {

    private static final Logger LOGGER = LoggerUtil.getLogger(HtmlTemplateProcessor.class);

    private final long startTime;
    private final AtomicLong pluginId = new AtomicLong(0);

    private final String baseUrl;

    private final HttpRequest request;

    private final AdminTokenVO adminTokenVO;

    private final String staticResourceBaseUrl;

    public HtmlTemplateProcessor(String baseUrl, HttpRequest request, AdminTokenVO adminTokenVO, String staticResourceBaseUrl) {
        this.baseUrl = baseUrl;
        this.request = request;
        this.adminTokenVO = adminTokenVO;
        this.startTime = request.getCreateTime();
        this.staticResourceBaseUrl = staticResourceBaseUrl;
    }

    public String transform(String htmlStr) throws Exception {
        Document document = Jsoup.parse(htmlStr, "", Parser.xmlParser());
        Map<String, String> replaceMap = new HashMap<>();
        for (Element tag : document.getAllElements()) {
            String tagName = tag.tagName();
            addStaticResourceFlag(tag, tagName);
            parseCustomHtmlTag(tag, tagName, replaceMap);
        }
        String html = document.html();
        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }
        String versionInfo = SecurityUtils.md5(html);
        if (BaseStaticSitePlugin.isStaticPluginRequest(request)) {
            return html + "<!--" + versionInfo + "-->";
        }
        return html + "<!--" + (System.currentTimeMillis() - startTime) + "ms(" + versionInfo + ")-->";
    }

    private void parseCustomHtmlTag(Element element, String tagName, Map<String, String> replaceMap) throws IOException, URISyntaxException, InterruptedException {
        if ("plugin".equals(tagName) && !element.attr("name").isEmpty()) {
            String url = "/" + element.attr("name") + "/" + element.attr("view").replaceFirst("/", "");
            if (!element.attr("param").isEmpty()) {
                url += "?" + element.attr("param");
            }
            element.attr("data-plugin-id", pluginId.incrementAndGet() + "");
            CloseResponseHandle handle = Constants.zrLogConfig.getPlugin(PluginCorePlugin.class).getContext(url, HttpMethod.GET, request, adminTokenVO);
            try (InputStream in = handle.getT().body()) {
                byte[] bytes = IOUtil.getByteByInputStream(in);
                if (handle.getStatusCode() != 200) {
                    throw new IOException("Template plugin page render status error " + handle.getStatusCode() + ", response body " + new String(bytes));
                }
                replaceMap.put(element.outerHtml(), new String(bytes, StandardCharsets.UTF_8));
            }
        }
    }

    private void addStaticResourceFlag(Element tag, String tagName) {
        if ("script".equals(tagName)) {
            String src = tag.attr("src");
            if (!src.isEmpty()) {
                tag.attr("src", tryReplace(src));
            }
        }
        if ("link".equals(tagName)) {
            String src = tag.attr("href");
            if (!src.isEmpty()) {
                tag.attr("href", tryReplace(src));
            }
        }
    }

    private String tryReplace(String href) {
        if (href.startsWith(baseUrl) || href.startsWith(staticResourceBaseUrl)) {
            String uriPath = href;
            //优先判断静态资源的情况
            if (href.startsWith(staticResourceBaseUrl)) {
                uriPath = href.substring(staticResourceBaseUrl.length());
            } else if (href.startsWith(baseUrl)) {
                uriPath = href.substring(baseUrl.length());
            }
            if (uriPath.contains("?")) {
                uriPath = uriPath.substring(0, uriPath.lastIndexOf("?"));
            }
            String flag = StaticFileCacheUtils.getInstance().getFileFlagFirstByCache(uriPath);
            if (flag != null) {
                if (href.contains("?")) {
                    href = href + "&t=" + flag;
                } else {
                    href = href + "?t=" + flag;
                }
            }
        }
        return href;
    }

}