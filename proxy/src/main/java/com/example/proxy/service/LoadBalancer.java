package com.example.proxy.service;

/**
 * Contract for selecting the next backend target URL.
 */
public interface LoadBalancer {

    String getNextUrl();
}
