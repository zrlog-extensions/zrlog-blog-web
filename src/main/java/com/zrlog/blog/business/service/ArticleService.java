package com.zrlog.blog.business.service;

import com.hibegin.common.dao.ResultValueConvertUtils;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.hibegin.common.util.ObjectHelpers;
import com.hibegin.common.util.StringUtils;
import com.hibegin.common.util.UrlEncodeUtils;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.blog.web.util.OutlineUtil;
import com.zrlog.blog.web.util.WebTools;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.business.util.ArticleHelpers;
import com.zrlog.common.Constants;
import com.zrlog.common.vo.Outline;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.model.Log;
import com.zrlog.util.I18nUtil;
import com.zrlog.util.ParseUtil;
import com.zrlog.util.ZrLogUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ArticleService {

    private List<ArticleDetailDTO.TagsDTO> getTags(ArticleDetailDTO log, HttpRequest request) {
        String keywords = ObjectHelpers.requireNonNullElse(log.getKeywords(), "");
        List<ArticleDetailDTO.TagsDTO> tags = new ArrayList<>();
        for (String tag : keywords.split(",")) {
            if (StringUtils.isEmpty(tag.trim())) {
                continue;
            }
            ArticleDetailDTO.TagsDTO map = new ArticleDetailDTO.TagsDTO();
            map.setName(tag);
            map.setUrl(WebTools.buildEncodedUrl(request, "tag/" + tag + StaticSitePlugin.getSuffix(request)));
            tags.add(map);
        }
        return tags;
    }

    private ArticleDetailDTO fillArticleInfo(ArticleDetailDTO log, HttpRequest request) {
        handlerArticle(log, request);
        String suffix = StaticSitePlugin.getSuffix(request);
        log.getNextLog().setUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + log.getNextLog().getAlias() + suffix));
        log.getLastLog().setUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + log.getLastLog().getAlias() + suffix));
        log.setTags(getTags(log, request));
        if (StringUtils.isNotEmpty(log.getMarkdown())) {
            List<Outline> outlineVO = OutlineUtil.extractOutline(log.getContent());
            if (!outlineVO.isEmpty()) {
                log.setTocHtml(OutlineUtil.buildTocHtml(outlineVO, ""));
            }
            log.setToc(outlineVO);
        }
        return log;
    }

    public ArticleDetailDTO detail(Object idOrAlias, HttpRequest request) throws SQLException {
        ArticleDetailDTO log = new Log().findByIdOrAlias(idOrAlias);
        if (Objects.isNull(log)) {
            return null;
        }
        if (log.getNextLog() == null) {
            ArticleDetailDTO.NextLogDTO nextLogDTO = new ArticleDetailDTO.NextLogDTO();
            nextLogDTO.setAlias(idOrAlias + "");
            nextLogDTO.setTitle(I18nUtil.getBlogStringFromRes("noNextLog"));
            log.setNextLog(nextLogDTO);
        }
        //
        if (log.getLastLog() == null) {
            ArticleDetailDTO.LastLogDTO lastLogDTO = new ArticleDetailDTO.LastLogDTO();
            lastLogDTO.setAlias(idOrAlias + "");
            lastLogDTO.setTitle(I18nUtil.getBlogStringFromRes("noLastLog"));
            log.setLastLog(lastLogDTO);
        }
        return fillArticleInfo(log, request);
    }

    public static <T extends ArticleBasicDTO> T handlerArticle(T log, HttpRequest request) {
        String suffix = StaticSitePlugin.getSuffix(request);
        String aliasUrl = UrlEncodeUtils.encodeUrl(log.getAlias()) + suffix;
        log.setAlias(aliasUrl);
        log.setRubbish(ResultValueConvertUtils.toBoolean(log.getRubbish()));
        log.setPrivacy(ResultValueConvertUtils.toBoolean(log.getPrivacy()));
        log.setHot(ResultValueConvertUtils.toBoolean(log.getHot()));
        PublicWebSiteInfo publicWebSiteInfo = Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo();
        log.setCanComment(ResultValueConvertUtils.toBoolean(log.getCanComment()) && Objects.equals(publicWebSiteInfo.getDisable_comment_status(), false));
        log.setUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + log.getAlias()));
        log.setTypeUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + "sort/" + log.getTypeAlias() + suffix));
        log.setNoSchemeUrl(ZrLogUtil.getHomeUrlWithHost(request) + Constants.getArticleUri() + UrlEncodeUtils.encodeUrl(log.getAlias()));
        log.setCommentUrl(ZrLogUtil.getHomeUrlWithHost(request) + Constants.getArticleUri() + "addComment");
        //
        log.setRecommended(ResultValueConvertUtils.toBoolean(log.getRecommended()));
        log.setReleaseTime(ResultValueConvertUtils.formatDate(log.getReleaseTime(), "yyyy-MM-dd"));
        if (Objects.nonNull(log.getLogId())) {
            log.setId(log.getLogId());
        }
        log.setLastUpdateDate(ResultValueConvertUtils.formatDate(log.getLast_update_date(), "yyyy-MM-dd"));
        log.setLast_update_date(ResultValueConvertUtils.formatDate(log.getLast_update_date(), "yyyy-MM-dd"));
        if (Objects.isNull(log.getDigest())) {
            log.setDigest("");
        }
        if (Objects.isNull(log.getContent())) {
            log.setContent("");
        }
        if (publicWebSiteInfo.getArticle_thumbnail_status() && StringUtils.isNotEmpty(log.getThumbnail())) {
            log.setThumbnailAlt(ParseUtil.removeHtmlElement(log.getTitle()));
        } else {
            log.setThumbnail(null);
            log.setThumbnailAlt(null);
        }
        return log;
    }

    public PageData<ArticleBasicDTO> pageByKeywords(PageRequest pageRequest, String keywords, HttpRequest request) {
        PageData<ArticleBasicDTO> data = new Log().visitorFind(pageRequest, keywords);
        ArticleHelpers.wrapperSearchKeyword(data, keywords);
        data.setKey(keywords);
        data.getRows().forEach(e -> handlerArticle(e, request));
        return data;
    }

}
