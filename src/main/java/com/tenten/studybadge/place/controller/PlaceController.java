package com.tenten.studybadge.place.controller;

import com.tenten.studybadge.place.dto.PlaceRequest;
import com.tenten.studybadge.place.dto.PlaceResponse;
import com.tenten.studybadge.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Place API", description = "오프라인 일정을 생성할 때 지도 api를 통해 장소를 선택하면 저장, 조회할 수 있는 API")
public class PlaceController {
  private final PlaceService placeService;

  @PostMapping("/study-channels/{studyChannelId}/places")
  @Operation(summary = "장소 저장", description = "지도 api에서 선택한 장소 저장" ,security = @SecurityRequirement(name = "bearerToken"))
  @Parameter(name = "studyChannelId", description = "일정을 만드는 study channel의 id 값", required = true)
  @Parameter(name = "PlaceRequest", description = "저장할 장소 request", required = true )
  public ResponseEntity<PlaceResponse> postPlace(
      @PathVariable Long studyChannelId,
      @Valid @RequestBody PlaceRequest placeRequest)  {
    return ResponseEntity.ok(placeService.postPlace(studyChannelId, placeRequest));
  }

}