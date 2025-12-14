package com.example.proxy.interfaces;

// Contract for selecting the next backend target URL.
public interface ILoadBalancer {

    String getNextUrl();
}
