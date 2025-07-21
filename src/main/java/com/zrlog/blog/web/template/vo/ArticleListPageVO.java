package com.zrlog.blog.web.template.vo;

import com.hibegin.common.dao.dto.PageData;
import com.zrlog.blog.web.util.PagerVO;
import com.zrlog.data.cache.vo.BaseDataInitVO;
import com.zrlog.data.dto.ArticleBasicDTO;

public class ArticleListPageVO extends BasePageInfo {

    private PageData<ArticleBasicDTO> data;
    private PagerVO pager;
    private String tipsType;
    private String tipsName;

    public ArticleListPageVO() {
    }

    public ArticleListPageVO(PageData<ArticleBasicDTO> data, BaseDataInitVO init) {
        this.data = data;
        this.init = init;
    }

    public PageData<ArticleBasicDTO> getData() {
        return data;
    }

    public void setData(PageData<ArticleBasicDTO> data) {
        this.data = data;
    }

    public PagerVO getPager() {
        return pager;
    }

    public void setPager(PagerVO pager) {
        this.pager = pager;
    }

    public String getTipsType() {
        return tipsType;
    }

    public void setTipsType(String tipsType) {
        this.tipsType = tipsType;
    }

    public String getTipsName() {
        return tipsName;
    }

    public void setTipsName(String tipsName) {
        this.tipsName = tipsName;
    }
}
