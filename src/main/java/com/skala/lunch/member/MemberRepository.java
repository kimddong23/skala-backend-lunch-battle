package com.skala.lunch.member;

import com.skala.lunch.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByLoginIdAndIdNot(String loginId, Long id);
    List<Member> findByDepartment(String department);
}
