package com.zrlog.blog;

import com.hibegin.common.dao.DataSourceWrapper;
import com.hibegin.http.server.WebServerBuilder;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.web.BlogWebSetup;
import com.zrlog.business.plugin.CacheManagerPlugin;
import com.zrlog.business.plugin.PluginCorePluginImpl;
import com.zrlog.common.Constants;
import com.zrlog.common.Updater;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.data.cache.CacheServiceImpl;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSetup;

import java.util.List;
import java.util.Objects;

import static com.zrlog.common.Constants.getZrLogHome;

public class Application {

    static {
        //System.setProperty("sws.run.mode", "dev");
        String home = getZrLogHome();
        if (Objects.nonNull(home)) {
            PathUtil.setRootPath(home);
        }
    }

    public static void main(String[] args) {
        Constants.zrLogConfig = new DevZrLogConfig(7080, null, "");
        WebServerBuilder build = new WebServerBuilder.Builder().config(Constants.zrLogConfig).build();
        build.addCreateSuccessHandle(() -> {
            Constants.zrLogConfig.startPluginsAsync();
            return null;
        });
        build.start();
    }
}

class DevZrLogConfig extends ZrLogConfig {

    protected DevZrLogConfig(Integer port, Updater updater, String contextPath) {
        super(port, updater, contextPath);
        webSetups.add(new BlogWebSetup(this, contextPath));
        webSetups.forEach(WebSetup::setup);
    }

    @Override
    public DataSourceWrapper configDatabase() throws Exception {
        this.dataSource = super.configDatabase();
        cacheService = new CacheServiceImpl();
        return dataSource;
    }

    @Override
    public void stop() {

    }

    @Override
    public void refreshPluginCacheData(String version, HttpRequest request) {

    }

    @Override
    public List<IPlugin> getBasePluginList() {
        Plugins plugins1 = new Plugins();
        plugins1.add(new PluginCorePluginImpl(dbPropertiesFile));
        plugins1.add(new CacheManagerPlugin(this));
        return plugins1;
    }
}
