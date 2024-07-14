package com.tenten.studybadge.study.channel.domain.entity;

import com.tenten.studybadge.common.BaseEntity;
import com.tenten.studybadge.member.domain.entity.Member;
import com.tenten.studybadge.study.channel.dto.StudyChannelDetailsResponse;
import com.tenten.studybadge.study.member.domain.entity.StudyMember;
import com.tenten.studybadge.type.study.channel.Category;
import com.tenten.studybadge.type.study.channel.MeetingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StudyChannel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_channel_id")
    private Long id;
    private String name;
    private String description;

    @Embedded
    private Recruitment recruitment;

    @Embedded
    private StudyDuration studyDuration;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private MeetingType meetingType;

    private String region;
    private String chattingUrl;
    private Integer viewCnt;
    private Integer deposit;

    @Builder.Default
    @OneToMany(mappedBy = "studyChannel", cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, orphanRemoval = true)
    private List<StudyMember> studyMembers = new ArrayList<>();

    public boolean isStartDateBeforeTo(LocalDate date) {
        return studyDuration.isStartDateBeforeTo(date);
    }

    public boolean isStudyMember(Long memberId) {
        return studyMembers.stream().anyMatch(studyMember -> studyMember.getMember().getId().equals(memberId));
    }

    public boolean isRecruitmentCompleted() {
        return recruitment.isCompleted();
    }

    public boolean isLeader(Member member) {
        return studyMembers.stream()
                .filter(studyMember -> studyMember.getMember().equals(member))
                .findFirst()
                .map(StudyMember::isLeader)
                .orElse(false);
    }

    public void addMember(Member member) {
        StudyMember studyMember = StudyMember.member(member, this);
        studyMembers.add(studyMember);
    }

    private StudyMember getLeader() {
        return studyMembers.stream()
                .filter(StudyMember::isLeader)
                .findFirst()
                .orElse(null);
    }

    private StudyMember getSubLeader() {
        return studyMembers.stream()
                .filter(StudyMember::isSubLeader)
                .findFirst()
                .orElse(null);
    }

    public StudyChannelDetailsResponse toResponse(Member member) {
        StudyMember leader = getLeader();
        StudyMember subLeader = getSubLeader();
        StudyChannelDetailsResponse.StudyChannelDetailsResponseBuilder builder = StudyChannelDetailsResponse.builder()
                .studyChannelId(this.id)
                .studyChannelName(this.name)
                .studyChannelDescription(this.description)
                .deposit(this.deposit)
                .category(this.category)
                .meetingType(this.meetingType)
                .region(this.region)
                .startDate(this.studyDuration.getStudyStartDate())
                .endDate(this.studyDuration.getStudyEndDate())
                .capacity(this.recruitment.getRecruitmentNumber())
                .leaderName(leader.getMember().getName())
                .subLeaderName(Objects.requireNonNullElse(subLeader, leader).getMember().getName());

        if (isStudyMember(member.getId())) {
            builder.chattingUrl(this.chattingUrl);
        }

        return builder.build();
    }

}
