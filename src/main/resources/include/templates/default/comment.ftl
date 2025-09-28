<#if log.canComment>
    <div class="comment" id="comment">
        <#if webs.changyan_status == "on">
            <plugin name="changyan" view="widget" param="articleId=${log.logId!''}"></plugin>
        <#else>
            <plugin name="comment" view="widget" param="articleId=${log.logId!''}"></plugin>
        </#if>
    </div>
</#if>
