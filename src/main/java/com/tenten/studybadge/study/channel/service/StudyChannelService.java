package com.tenten.studybadge.study.channel.service;

import com.tenten.studybadge.common.exception.studychannel.InvalidStudyStartDateException;
import com.tenten.studybadge.study.channel.domain.entity.StudyChannel;
import com.tenten.studybadge.study.channel.domain.repository.StudyChannelRepository;
import com.tenten.studybadge.study.channel.dto.StudyChannelCreateRequest;
import com.tenten.studybadge.study.member.domain.entity.StudyMember;
import com.tenten.studybadge.study.member.domain.repository.StudyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StudyChannelService {

    private final StudyChannelRepository studyChannelRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional
    public Long create(StudyChannelCreateRequest request, Long memberId) {

        StudyChannel studyChannel = request.toEntity();

        if (studyChannel.isStartDateBeforeTo(LocalDate.now(Clock.systemDefaultZone()))) {
            throw new InvalidStudyStartDateException();
        }

        StudyMember studyMember = StudyMember.leader(memberId, studyChannel);

        studyChannelRepository.save(studyChannel);
        studyMemberRepository.save(studyMember);

        return studyChannel.getId();
    }

}