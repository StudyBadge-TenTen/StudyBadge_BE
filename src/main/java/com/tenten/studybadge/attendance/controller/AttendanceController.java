package com.tenten.studybadge.attendance.controller;

import com.tenten.studybadge.attendance.dto.AttendanceCheckRequest;
import com.tenten.studybadge.attendance.dto.AttendanceInfoResponse;
import com.tenten.studybadge.attendance.service.AttendanceService;
import com.tenten.studybadge.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Attendance API", description = "출석과 관련된 기능을 제공하는 API")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/api/study-channels/{studyChannelId}/check-attendance")
    @Operation(summary = "출석 체크 및 갱신", description = "출석 체크와 출석 상태를 변경하는 API", security = @SecurityRequirement(name = "bearerToken"))
    @Parameter(name = "studyChannelId", description = "스터디 채널 ID", required = true)
    @Parameter(name = "attendanceCheckRequest", description = "출석 체크를 위해 필요한 요청 데이터 모음", required = true)
    public ResponseEntity<Void> checkAttendance(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long studyChannelId,
            @Valid @RequestBody AttendanceCheckRequest attendanceCheckRequest) {

        attendanceService.checkAttendance(attendanceCheckRequest, principal.getId(), studyChannelId);
        return ResponseEntity.ok().build();

    }

    @GetMapping("/api/study-channels/{studyChannelId}/attendances")
    @Operation(summary = "출석 현황 조회", description = "스터디 채널 내 멤버별 출석 현황을 조회하는 API", security = @SecurityRequirement(name = "bearerToken"))
    @Parameter(name = "studyChannelId", description = "스터디 채널 ID", required = true)
    public ResponseEntity<List<AttendanceInfoResponse>> getAttendanceRatio(@PathVariable Long studyChannelId, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(attendanceService.getAttendanceRatioForStudyChannel(studyChannelId, principal.getId()));
    }

}
