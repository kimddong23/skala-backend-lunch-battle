package com.skala.lunch.review;

import com.skala.lunch.review.ReviewDto;
import com.skala.lunch.member.Member;
import com.skala.lunch.restaurant.Restaurant;
import com.skala.lunch.review.Review;
import com.skala.lunch.global.error.NotFoundException;
import com.skala.lunch.member.MemberRepository;
import com.skala.lunch.restaurant.RestaurantRepository;
import com.skala.lunch.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final RestaurantRepository restaurantRepository;

    /**
     * 리뷰 작성 또는 수정.
     *
     * 한 사람이 한 식당에 리뷰를 여러 개 남기면 평점이 왜곡된다.
     * 이미 남긴 리뷰가 있으면 새로 만들지 않고 고친다.
     */
    @Transactional
    public ReviewDto write(ReviewDto dto) {
        Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new NotFoundException("사원을 찾을 수 없습니다: " + dto.getMemberId()));
        Restaurant restaurant = restaurantRepository.findById(dto.getRestaurantId())
                .orElseThrow(() -> new NotFoundException("식당을 찾을 수 없습니다: " + dto.getRestaurantId()));

        Review review = reviewRepository
                .findByMemberIdAndRestaurantId(dto.getMemberId(), dto.getRestaurantId())
                .orElseGet(() -> Review.builder()
                        .member(member)
                        .restaurant(restaurant)
                        .build());

        review.setScore(dto.getScore());
        review.setComment(dto.getComment());

        return toDto(reviewRepository.save(review));
    }

    public List<ReviewDto> getByRestaurant(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new NotFoundException("식당을 찾을 수 없습니다: " + restaurantId);
        }
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public ReviewDto get(Long id) {
        return toDto(reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다: " + id)));
    }

    @Transactional
    public void delete(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다: " + id));
        reviewRepository.delete(review);
    }

    private ReviewDto toDto(Review r) {
        return ReviewDto.builder()
                .id(r.getId())
                .memberId(r.getMember().getId())
                .memberName(r.getMember().getName())
                .restaurantId(r.getRestaurant().getId())
                .restaurantName(r.getRestaurant().getName())
                .score(r.getScore())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
