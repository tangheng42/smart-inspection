package com.zhixunyun.service;

import java.time.LocalDateTime;

public interface InspectionTaskGenerationService {
    boolean generateOnePlan(Long planId, LocalDateTime now);

    int markOverdue(LocalDateTime now);
}
