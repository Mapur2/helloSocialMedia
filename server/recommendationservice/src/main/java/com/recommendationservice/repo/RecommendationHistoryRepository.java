package com.recommendationservice.repo;


import com.recommendationservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, String> {

    List<RecommendationHistory> findByUserIdAndItemType(String userId, RecommendationItemType itemType);
}
