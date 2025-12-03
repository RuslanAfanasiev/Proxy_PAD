package com.example.proxy.controller;

import com.example.proxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;

    @GetMapping("/**")
    public String forwardGet(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String pathAfterProxy = fullPath.substring((contextPath + "/proxy").length());
        String query = request.getQueryString();

        String pathAndQuery = query == null ? pathAfterProxy : pathAfterProxy + "?" + query;
        return proxyService.forwardGet(pathAndQuery);
    }
}
