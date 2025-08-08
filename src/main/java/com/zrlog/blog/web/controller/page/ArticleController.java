package com.zrlog.blog.web.controller.page;

import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.hibegin.common.dao.dto.PageRequestImpl;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.annotation.RequestMethod;
import com.hibegin.http.annotation.ResponseBody;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.api.HttpResponse;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.rest.request.CreateCommentRequest;
import com.zrlog.blog.business.rest.response.CreateCommentResponse;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.business.service.CommentService;
import com.zrlog.blog.web.util.PagerUtil;
import com.zrlog.blog.web.util.PagerVO;
import com.zrlog.blog.web.util.WebTools;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.model.Log;
import com.zrlog.util.I18nUtil;
import com.zrlog.util.ParseUtil;
import com.zrlog.util.ZrLogUtil;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class ArticleController extends Controller {

    private static final Logger LOGGER = LoggerUtil.getLogger(ArticleController.class);
    private final CommentService commentService = new CommentService();
    private final ArticleService articleService = new ArticleService();
    private final CacheService cacheService;

    public ArticleController(HttpRequest request, HttpResponse response) {
        super(request, response);
        this.cacheService = Constants.zrLogConfig.getCacheService();
    }

    public ArticleController() {
        this.cacheService = Constants.zrLogConfig.getCacheService();
    }

    /**
     * add page info for template more easy
     */
    private void setPageDataInfo(String currentUri, PageData<ArticleBasicDTO> data, PageRequest pageRequest) {
        data.getRows().forEach(e -> ArticleService.handlerArticle(e, request));
        getRequest().getAttr().put("yurl", Constants.getArticleUri() + currentUri);
        long totalPage = BigDecimal.valueOf(Math.ceil(data.getTotalElements() * 1.0 / pageRequest.getSize())).longValue();
        if (totalPage > 0) {
            getRequest().getAttr().put("data", data);
            //大于1页
            if (totalPage > 1) {
                PagerVO pager = PagerUtil.generatorPager(currentUri, pageRequest.getPage(), totalPage);
                String suffix = StaticSitePlugin.getSuffix(request);
                for (PagerVO.PageEntry pageMap : pager.getPageList()) {
                    pageMap.setUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + pageMap.getUrl()) + suffix);
                }
                pager.setPageStartUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + pager.getPageStartUrl() + suffix));
                pager.setPageEndUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + pager.getPageEndUrl() + suffix));
                //pager
                getRequest().getAttr().put("pager", pager);
            }
        }

    }

    @RequestMethod
    public String index() {
        PageRequest pageRequest = new PageRequestImpl(parseUriInfo(request.getUri()).getPage(), cacheService.getPublicWebSiteInfo().getRows());
        PageData<ArticleBasicDTO> data = new Log().visitorFind(pageRequest, null);
        setPageDataInfo("all-", data, pageRequest);
        return "index";
    }

    public String search() {
        String key = request.getParaToStr("key", "");
        if (StringUtils.isEmpty(key) && request.getMethod() == HttpMethod.POST) {
            return index();
        }
        ArticleUriInfoVO uriInfoVO = parseUriInfo(request.getUri());
        PageData<ArticleBasicDTO> data;
        if (StringUtils.isEmpty(key) && request.getMethod() == HttpMethod.GET) {
            try {
                key = uriInfoVO.getKey();
            } catch (Exception e) {
                LOGGER.warning("Parse " + request.getUri() + " error " + e.getMessage());
            }
        }
        if (StringUtils.isEmpty(key)) {
            return index();
        }
        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        data = new Log().visitorFind(new PageRequestImpl(1L, rows), key);
        // 记录回话的Key
        request.getAttr().put("key", WebTools.htmlEncode(key));
        request.getAttr().put("tipsType", I18nUtil.getBlogStringFromRes("search"));
        request.getAttr().put("tipsName", WebTools.htmlEncode(key));

        setPageDataInfo("search/" + key + "-", data, new PageRequestImpl(uriInfoVO.getPage(), rows));
        return "page";
    }

    @RequestMethod
    public String record() {
        ArticleUriInfoVO uriInfoVO = parseUriInfo(request.getUri());
        request.getAttr().put("tipsType", I18nUtil.getBlogStringFromRes("archive"));
        request.getAttr().put("tipsName", uriInfoVO.getKey());

        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        setPageDataInfo("record/" + uriInfoVO.getKey() + "-", new Log().findByDate(uriInfoVO.getPage(), rows, uriInfoVO.getKey()), new PageRequestImpl(uriInfoVO.getPage(), rows));
        return "page";
    }

    @RequestMethod(method = HttpMethod.POST)
    public void addComment() throws SQLException {
        CreateCommentResponse x = saveComment();
        String ext = "";
        if (Constants.isStaticHtmlStatus()) {
            ext = ".html";
            if (!Constants.zrLogConfig.getServerConfig().isNativeImageAgent()) {
                cacheService.refreshInitData();
            }
        }
        response.redirect("/" + Constants.getArticleUri() + x.getAlias() + ext);
    }

    @RequestMethod(method = HttpMethod.POST)
    @ResponseBody
    public CreateCommentResponse saveComment() throws SQLException {
        CreateCommentRequest createCommentRequest = ZrLogUtil.convertRequestParam(getRequest().decodeParamMap(), CreateCommentRequest.class);
        createCommentRequest.setIp(WebTools.getRealIp(getRequest()));
        createCommentRequest.setUserAgent(Jsoup.clean(request.getHeader("User-Agent"), Safelist.basic()));
        return commentService.save(createCommentRequest);
    }

    @RequestMethod
    public String detail() throws SQLException {
        String uri = getRequest().getUri();
        ArticleDetailDTO detail = articleService.detail(uri.replace("/", "").replace(".html", ""), request);
        if (Objects.nonNull(detail)) {
            request.getAttr().put("log", detail);
        }
        return "detail";
    }


    private static ArticleUriInfoVO parseUriInfo(String uri) {
        String rawUrl = uri;
        if (rawUrl.endsWith(".html")) {
            rawUrl = rawUrl.substring(0, rawUrl.length() - ".html".length());
        }
        String rawArgs = rawUrl.substring(rawUrl.lastIndexOf("/") + 1);
        List<String> list = Arrays.asList(rawArgs.split("-"));
        boolean numeric = ParseUtil.isNumeric(list.get(list.size() - 1));
        StringJoiner sj = new StringJoiner("-");
        int page = 1;
        if (numeric) {
            page = ParseUtil.strToInt(list.get(list.size() - 1), 1);
            for (int i = 0; i < list.size() - 1; i++) {
                sj.add(list.get(i));
            }
        } else {
            for (String arg : list) {
                sj.add(arg);
            }
        }
        String key = sj.toString();
        return new ArticleUriInfoVO(key, page);
    }

    public static void main(String[] args) {
        ArticleUriInfoVO uriInfoVO = parseUriInfo("/record/2015-06.html");
        System.out.println(uriInfoVO);
    }

    @RequestMethod
    public String sort() throws SQLException {
        ArticleUriInfoVO uriInfoVO = parseUriInfo(request.getUri());
        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        setPageDataInfo("sort/" + uriInfoVO.getKey() + "-", new Log().findByTypeAlias(uriInfoVO.getPage(), rows, uriInfoVO.getKey()), new PageRequestImpl(uriInfoVO.getPage(), rows));
        request.getAttr().put("tipsType", I18nUtil.getBlogStringFromRes("category"));
        Optional<TypeDTO> first = cacheService.getArticleTypes().stream().filter(e -> Objects.equals(e.getAlias(), uriInfoVO.getKey())).findFirst();
        if (first.isPresent()) {
            request.getAttr().put("tipsName", first.get().getTypeName());
        } else {
            request.getAttr().put("tipsName", "");
        }
        return "page";
    }

    @RequestMethod
    public String tag() throws SQLException {
        ArticleUriInfoVO uriInfoVO = parseUriInfo(request.getUri());
        String tag = uriInfoVO.getKey();
        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        setPageDataInfo("tag/" + tag + "-", new Log().findByTag(uriInfoVO.getPage(), rows, tag), new PageRequestImpl(uriInfoVO.getPage(), rows));
        getRequest().getAttr().put("tipsType", I18nUtil.getBlogStringFromRes("tag"));
        getRequest().getAttr().put("tipsName", tag);
        return "page";
    }

    @RequestMethod
    public String tags() {
        return "tags";
    }

    @RequestMethod
    public String link() {
        return "link";
    }

}
