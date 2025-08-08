package com.zrlog.blog.business.service;

import com.hibegin.common.dao.ResultBeanUtils;
import com.hibegin.common.util.StringUtils;
import com.zrlog.blog.business.rest.request.CreateCommentRequest;
import com.zrlog.blog.business.rest.response.CreateCommentResponse;
import com.zrlog.common.Constants;
import com.zrlog.common.exception.ArgsException;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.model.Comment;
import com.zrlog.model.Log;
import com.zrlog.util.ParseUtil;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class CommentService {

    private static boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    private boolean isAllowComment(ArticleBasicDTO articleBasicDTO) {
        if (Constants.zrLogConfig.getCacheService().getPublicWebSiteInfo().getDisable_comment_status()) {
            return false;
        }
        return Objects.equals(articleBasicDTO.getCanComment(), true);
    }

    public CreateCommentResponse save(CreateCommentRequest createCommentRequest) throws SQLException {
        if (Objects.isNull(createCommentRequest.getLogId())) {
            throw new ArgsException("logId");
        }
        if (Objects.isNull(createCommentRequest.getComment())) {
            throw new ArgsException("comment");
        }
        String email = createCommentRequest.getMail();
        if (StringUtils.isNotEmpty(email) && !isValidEmailAddress(email)) {
            throw new IllegalArgumentException(email + "not email address");
        }
        String nickname = createCommentRequest.getUserName();
        if (StringUtils.isEmpty(nickname)) {
            throw new IllegalArgumentException("nickname not block");
        }
        Map<String, Object> dbLog = new Log().loadById(createCommentRequest.getLogId());
        if (Objects.isNull(dbLog)) {
            return new CreateCommentResponse(createCommentRequest.getLogId());
        }
        ArticleBasicDTO log = ResultBeanUtils.convert(dbLog, ArticleBasicDTO.class);
        if (!isAllowComment(log)) {
            return new CreateCommentResponse(log.getAlias());
        }
        nickname = Jsoup.clean(createCommentRequest.getUserName(), Safelist.basic());
        String userHome = createCommentRequest.getUserHome();
        if (StringUtils.isNotEmpty(userHome)) {
            userHome = Jsoup.clean(createCommentRequest.getUserHome(), Safelist.basic());
        }
        String comment = Jsoup.clean(createCommentRequest.getComment(), Safelist.basic());
        if (StringUtils.isNotEmpty(comment) && !ParseUtil.isGarbageComment(comment)) {
            new Comment().set("userHome", userHome).set("userMail", email)
                    .set("userIp", createCommentRequest.getIp())
                    .set("userName", nickname)
                    .set("logId", createCommentRequest.getLogId())
                    .set("userComment", comment)
                    .set("user_agent", createCommentRequest.getUserAgent())
                    .set("reply_id", createCommentRequest.getReplyId())
                    .set("commTime", new Date())
                    .set("hide", 1)
                    .save();
        }
        return new CreateCommentResponse(log.getAlias());
    }
}
