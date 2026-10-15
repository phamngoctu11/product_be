package com.example.workflow.nume;

public enum OrderItemProductionStatus {
    NOT_REQUIRED,
    WAITING_ASSIGNMENT,
    ASSIGNED,
    INITIAL_IN_PROGRESS,
    FINAL_IN_PROGRESS,
    FINAL_WAITING_QC,
    FINAL_REWORK_REQUIRED,
    READY
}
