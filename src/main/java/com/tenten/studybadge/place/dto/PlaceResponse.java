package com.tenten.studybadge.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceResponse {
  private long id;
  private double lat;
  private double lng;
  private String placeName;
  private String placeAddress;
}