package com.example.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderWorkflowService {
    private final RuntimeService runtimeService;

    public boolean deleteProcessIfExists(Long orderId, String reason) {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .variableValueEquals("orderId", orderId)
                    .singleResult();
            if (processInstance == null) {
                return false;
            }
            runtimeService.deleteProcessInstance(processInstance.getId(), reason);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Could not delete workflow process for order {}: {}", orderId, ex.getMessage());
            return false;
        }
    }
}
