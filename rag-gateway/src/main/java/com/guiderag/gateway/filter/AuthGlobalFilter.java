package com.guiderag.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guiderag.common.constant.AuthConstants;
import com.guiderag.common.utils.JwtUtils;
import com.guiderag.gateway.config.WhiteListConfig;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final WhiteListConfig whiteListConfig;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public AuthGlobalFilter(WhiteListConfig whiteListConfig) {
        this.whiteListConfig = whiteListConfig;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 匹配白名单（如 /auth/**）
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 获取 Header 中的 Token
        String token = getToken(request);
        if (token == null || !token.startsWith(AuthConstants.TOKEN_PREFIX)) {
            return unauthorizedResponse(exchange, "未携带有效的认证 Token");
        }

        token = token.substring(AuthConstants.TOKEN_PREFIX.length());
        
        // 校验 Token
        Long userId = JwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            return unauthorizedResponse(exchange, "Token 无效或已过期，请重新登录");
        }

        // 解析成功后，将用户信息放置在 Header 中透传至下游业务服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(AuthConstants.USER_ID_HEADER, userId.toString())
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        return chain.filter(mutatedExchange);
    }

    private boolean isWhiteList(String path) {
        if (whiteListConfig.getUrls() == null) {
            return false;
        }
        for (String url : whiteListConfig.getUrls()) {
            if (antPathMatcher.match(url, path)) {
                return true;
            }
        }
        return false;
    }

    private String getToken(ServerHttpRequest request) {
        return request.getHeaders().getFirst(AuthConstants.AUTH_HEADER);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // 统一返回类似 Result 的 JSON 数据
        String jsonResult = "{\"code\": 401, \"message\": \"" + message + "\", \"data\": null}";
        DataBuffer buffer = response.bufferFactory().wrap(jsonResult.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // 最优先拦截
    }
}
