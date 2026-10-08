package com.example.workflow.util;

import com.example.workflow.entity.ConsultationSaleAttribution;
import com.example.workflow.event.payload.CommissionRefreshKey;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class CommissionRefreshKeys {
    public Set<CommissionRefreshKey> fromAttributions(Collection<ConsultationSaleAttribution> attributions) {
        Set<CommissionRefreshKey> result = new HashSet<>();
        if (attributions == null) {
            return result;
        }
        for (ConsultationSaleAttribution attribution : attributions) {
            if (attribution == null || attribution.getStaff() == null || attribution.getStaff().getId() == null) {
                continue;
            }
            String staffId = attribution.getStaff().getId();
            add(result, staffId, attribution.getOrderCreatedAt());
            add(result, staffId, attribution.getConfirmedAt());
            add(result, staffId, attribution.getCancelledAt());
        }
        return result;
    }

    private void add(Set<CommissionRefreshKey> keys, String staffId, LocalDateTime dateTime) {
        if (dateTime != null) {
            keys.add(new CommissionRefreshKey(staffId, dateTime.toLocalDate().toString()));
        }
    }
}
