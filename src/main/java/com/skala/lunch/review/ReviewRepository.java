package com.skala.lunch.review;

import com.skala.lunch.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByMemberIdAndRestaurantId(Long memberId, Long restaurantId);
    List<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    long countByRestaurantId(Long restaurantId);
    long countByMemberId(Long memberId);
}
