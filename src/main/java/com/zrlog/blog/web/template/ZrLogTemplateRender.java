package com.zrlog.blog.web.template;

import com.hibegin.common.util.StringUtils;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.util.FreeMarkerUtil;
import com.hibegin.http.server.util.PathUtil;
import com.hibegin.template.TemplateRender;
import com.zrlog.blog.web.plugin.TemplateDownloadPlugin;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.business.plugin.BodySaveResponse;
import com.zrlog.business.template.HtmlTemplateProcessor;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.exception.NotImplementException;
import com.zrlog.common.vo.AdminTokenVO;
import com.zrlog.plugin.BaseStaticSitePlugin;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public class ZrLogTemplateRender implements TemplateRender {

    private final HttpRequest request;

    private final BasePageInfo pageInfo;

    public ZrLogTemplateRender(HttpRequest request) throws Exception {
        this.request = request;
        this.pageInfo = TemplateRenderUtils.fullTemplateInfo(request);
        setupTemplate();
    }

    private void setupTemplate() throws Exception {
        if (Objects.equals(pageInfo.getTemplate(), Constants.DEFAULT_TEMPLATE_PATH)) {
            FreeMarkerUtil.initClassTemplate(Constants.DEFAULT_TEMPLATE_PATH);
            return;
        }
        TemplateDownloadPlugin templateDownloadPlugin = Constants.zrLogConfig.getPlugin(TemplateDownloadPlugin.class);
        if (Objects.nonNull(templateDownloadPlugin)) {
            templateDownloadPlugin.precheckTemplate(pageInfo.getTemplate());
        }
        File path = PathUtil.getStaticFile(pageInfo.getTemplate());
        if (path.exists()) {
            FreeMarkerUtil.init(path.getPath());
            return;
        }
        throw new RuntimeException("missing template files -> " + path.getName());
    }

    private boolean existsByTemplateName(String templateName) {
        File path = PathUtil.getStaticFile(pageInfo.getTemplate());
        if (path.exists() && !Objects.equals(pageInfo.getTemplate(), Constants.DEFAULT_TEMPLATE_PATH)) {
            return Arrays.stream(Objects.requireNonNull(path.listFiles())).anyMatch(e -> e.getName().startsWith(templateName + "."));
        } else {
            return Objects.nonNull(ZrLogTemplateRender.class.getResourceAsStream(Constants.DEFAULT_TEMPLATE_PATH + "/" + templateName + ".ftl"));
        }
    }

    @Override
    public String render(InputStream inputStream) {
        throw new NotImplementException();
    }

    @Override
    public String render(String s) {
        throw new NotImplementException();
    }

    /**
     * 处理静态化文件
     */
    private static boolean catGeneratorHtml(HttpRequest request) {
        if (BaseStaticSitePlugin.isStaticPluginRequest(request)) {
            return false;
        }
        String targetUri = request.getUri();
        if (!Constants.isStaticHtmlStatus()) {
            return false;
        }
        return targetUri.endsWith(".html");
    }

    @Override
    public String renderByTemplateName(String page) throws Exception {
        if (StringUtils.isNotEmpty(pageInfo.getArrangePlugin()) && existsByTemplateName("arrange")) {
            page = "arrange";
        }
        String htmlStr = FreeMarkerUtil.renderToFMByModel(page, pageInfo);
        TokenService tokenService = Constants.zrLogConfig.getTokenService();
        AdminTokenVO adminTokenVO = null;
        if (Objects.nonNull(tokenService)) {
            adminTokenVO = tokenService.getAdminTokenVO(request);
        }
        HtmlTemplateProcessor pluginTagHande = new HtmlTemplateProcessor(
                request, adminTokenVO, Objects.requireNonNullElse(pageInfo.getStaticResourceBaseUrl(), "/"));
        String realHtmlStr = pluginTagHande.transform(htmlStr);
        if (!catGeneratorHtml(request)) {
            return realHtmlStr;
        }
        //save to cache
        try (BodySaveResponse bodySaveResponse = new BodySaveResponse(request, Constants.zrLogConfig.getResponseConfig(), false)) {
            bodySaveResponse.renderHtmlStr(realHtmlStr);
        }
        return realHtmlStr;
    }
}
