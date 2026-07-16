package com.example.workflow.event.payload;

import java.util.Set;

public record StaffCommissionRefreshRequestedEvent(Set<CommissionRefreshKey> refreshKeys) {
}
