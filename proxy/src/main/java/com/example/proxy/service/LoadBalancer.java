package com.example.proxy.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LoadBalancer service implements Round-Robin algorithm
 * for distributing requests across multiple Data Warehouse nodes.
 */
@Service
@Slf4j
public class LoadBalancer {

    @Value("${proxy.datawarehouse.nodes}")
    private String dataWarehouseNodes;

    private List<String> nodes;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        nodes = Arrays.asList(dataWarehouseNodes.split(","));
        log.info("LoadBalancer initialized with nodes: {}", nodes);
    }

    /**
     * Get next available Data Warehouse node using Round-Robin algorithm.
     * Thread-safe implementation using AtomicInteger.
     *
     * @return URL of the next Data Warehouse node
     */
    public String getNextNode() {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("No Data Warehouse nodes available");
        }

        int index = currentIndex.getAndUpdate(i -> (i + 1) % nodes.size());
        String selectedNode = nodes.get(index);

        log.debug("Selected node: {} (index: {})", selectedNode, index);
        return selectedNode;
    }

    /**
     * Get all available nodes.
     *
     * @return List of all Data Warehouse nodes
     */
    public List<String> getAllNodes() {
        return nodes;
    }
}
