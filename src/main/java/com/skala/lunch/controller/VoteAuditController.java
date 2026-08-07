package com.skala.lunch.controller;

import com.skala.lunch.entity.VoteAuditLog;
import com.skala.lunch.repository.VoteAuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AOP 가 남긴 투표 감사 로그 조회.
 * 투표 API 어디에도 기록 코드가 없는데 이 목록이 쌓이는 것이 AOP 적용의 증거다.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "6. AOP 감사 로그", description = "AOP 가 자동으로 남긴 투표 기록")
public class VoteAuditController {

    private final VoteAuditLogRepository repository;

    @GetMapping
    @Operation(summary = "감사 로그 조회", description = "투표 성공 시 VoteAuditAspect 가 남긴 기록")
    public ResponseEntity<List<VoteAuditLog>> getAll() {
        return ResponseEntity.ok(repository.findAll(Sort.by(Sort.Direction.DESC, "id")));
    }
}
