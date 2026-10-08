package com.example.workflow.mapper;

import com.example.workflow.dto.ReputationHistoryDTO;
import com.example.workflow.entity.ReputationHistory;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface ReputationHistoryMapper {
    ReputationHistoryDTO toDto(ReputationHistory history);
}
