package com.tenten.studybadge.schedule.domain.entity;


import com.tenten.studybadge.schedule.domain.Schedule;
import com.tenten.studybadge.study.channel.domain.entity.StudyChannel;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SingleSchedule extends Schedule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  private boolean isRepeated;
  @Setter
  private Long placeId;

  @Setter
  @ManyToOne
  @JoinColumn(name = "study_channel_id", nullable = false)
  private StudyChannel studyChannel;

  @Builder(builderMethodName = "withoutIdBuilder")
  public SingleSchedule(String scheduleName, String scheduleContent, LocalDate scheduleDate, LocalTime scheduleStartTime,
      LocalTime scheduleEndTime, boolean isRepeated, Long placeId, StudyChannel studyChannel) {
    this.scheduleName = scheduleName;
    this.scheduleContent = scheduleContent;
    this.scheduleDate = scheduleDate;
    this.scheduleStartTime = scheduleStartTime;
    this.scheduleEndTime = scheduleEndTime;
    this.isRepeated = isRepeated;
    this.placeId = placeId;
    this.studyChannel = studyChannel;
  }

  @Builder(builderMethodName = "withIdBuilder")
  public SingleSchedule(long scheduleId, String scheduleName, String scheduleContent, LocalDate scheduleDate, LocalTime scheduleStartTime,
      LocalTime scheduleEndTime, boolean isRepeated, Long placeId, StudyChannel studyChannel) {
    this.id = scheduleId;
    this.scheduleName = scheduleName;
    this.scheduleContent = scheduleContent;
    this.scheduleDate = scheduleDate;
    this.scheduleStartTime = scheduleStartTime;
    this.scheduleEndTime = scheduleEndTime;
    this.isRepeated = isRepeated;
    this.placeId = placeId;
    this.studyChannel = studyChannel;
  }

}
