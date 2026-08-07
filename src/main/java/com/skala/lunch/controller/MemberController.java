package com.skala.lunch.controller;

import com.skala.lunch.dto.MemberDto;
import com.skala.lunch.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "1. 사원", description = "투표에 참여하는 사원 관리")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "사원 등록")
    public ResponseEntity<MemberDto> create(@Valid @RequestBody MemberDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(dto));
    }

    @GetMapping
    @Operation(summary = "전체 사원 조회")
    public ResponseEntity<List<MemberDto>> getAll() {
        return ResponseEntity.ok(memberService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "사원 단건 조회")
    public ResponseEntity<MemberDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.get(id));
    }

    @GetMapping("/department/{department}")
    @Operation(summary = "부서별 사원 조회")
    public ResponseEntity<List<MemberDto>> byDepartment(@PathVariable String department) {
        return ResponseEntity.ok(memberService.getByDepartment(department));
    }

    @PutMapping("/{id}")
    @Operation(summary = "사원 수정")
    public ResponseEntity<MemberDto> update(@PathVariable Long id, @Valid @RequestBody MemberDto dto) {
        return ResponseEntity.ok(memberService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사원 삭제", description = "투표·리뷰 기록이 있으면 409")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
