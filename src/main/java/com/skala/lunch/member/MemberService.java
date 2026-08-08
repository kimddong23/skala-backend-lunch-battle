package com.skala.lunch.member;

import com.skala.lunch.member.MemberDto;
import com.skala.lunch.member.Member;
import com.skala.lunch.global.error.ConflictException;
import com.skala.lunch.global.error.NotFoundException;
import com.skala.lunch.member.MemberRepository;
import com.skala.lunch.review.ReviewRepository;
import com.skala.lunch.battle.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final VoteRepository voteRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public MemberDto create(MemberDto dto) {
        if (memberRepository.existsByLoginId(dto.getLoginId())) {
            throw new ConflictException("이미 존재하는 로그인 ID 입니다: " + dto.getLoginId());
        }
        Member saved = memberRepository.save(Member.builder()
                .loginId(dto.getLoginId())
                .name(dto.getName())
                .department(dto.getDepartment())
                .build());
        return toDto(saved);
    }

    public MemberDto get(Long id) {
        return toDto(find(id));
    }

    public List<MemberDto> getAll() {
        return memberRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<MemberDto> getByDepartment(String department) {
        return memberRepository.findByDepartment(department).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    /**
     * 수정. 로그인 ID 중복 검사에서 자기 자신은 제외한다.
     * 제외하지 않으면 값을 바꾸지 않고 그대로 저장하는 요청이 중복으로 거부된다.
     */
    @Transactional
    public MemberDto update(Long id, MemberDto dto) {
        Member member = find(id);
        if (memberRepository.existsByLoginIdAndIdNot(dto.getLoginId(), id)) {
            throw new ConflictException("이미 존재하는 로그인 ID 입니다: " + dto.getLoginId());
        }
        member.setLoginId(dto.getLoginId());
        member.setName(dto.getName());
        member.setDepartment(dto.getDepartment());
        return toDto(memberRepository.save(member));
    }

    /** 투표·리뷰가 남아 있으면 지우지 않는다. 기록은 보존 대상이다. */
    @Transactional
    public void delete(Long id) {
        Member member = find(id);
        long votes = voteRepository.countByMemberId(id);
        long reviews = reviewRepository.countByMemberId(id);
        if (votes > 0 || reviews > 0) {
            throw new ConflictException(
                    "투표나 리뷰 기록이 있는 사원은 삭제할 수 없습니다"
                            + " (투표 " + votes + "건, 리뷰 " + reviews + "건)");
        }
        memberRepository.delete(member);
    }

    private Member find(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("사원을 찾을 수 없습니다: " + id));
    }

    private MemberDto toDto(Member m) {
        return MemberDto.builder()
                .id(m.getId())
                .loginId(m.getLoginId())
                .name(m.getName())
                .department(m.getDepartment())
                .build();
    }
}
