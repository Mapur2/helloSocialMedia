package com.recommendationservice.repo;

import com.recommendationservice.entity.RecommendationHistory;
import com.recommendationservice.entity.RecommendationItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, String> {

    List<RecommendationHistory> findByUserIdAndItemType(String userId, RecommendationItemType itemType);

    List<RecommendationHistory> findByUserIdAndItemTypeOrderByCreatedAtDesc(String userId, RecommendationItemType itemType);
}
