package com.guiderag.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "gateway.whitelist")
public class WhiteListConfig {
    private List<String> paths;
    private List<String> urls;

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public List<String> getUrls() {
        return urls != null ? urls : paths;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}
