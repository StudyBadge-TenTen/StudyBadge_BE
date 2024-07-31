package com.tenten.studybadge.schedule.service;

import static com.tenten.studybadge.common.constant.NotificationConstant.REPEAT_SCHEDULE_CREATE;
import static com.tenten.studybadge.common.constant.NotificationConstant.REPEAT_SCHEDULE_DELETE;
import static com.tenten.studybadge.common.constant.NotificationConstant.SCHEDULE_RELATED_URL;
import static com.tenten.studybadge.common.constant.NotificationConstant.SCHEDULE_UPDATE_FOR_REPEAT_TO_REPEAT;
import static com.tenten.studybadge.common.constant.NotificationConstant.SCHEDULE_UPDATE_FOR_REPEAT_TO_SINGLE;
import static com.tenten.studybadge.common.constant.NotificationConstant.SCHEDULE_UPDATE_FOR_SINGLE_TO_REPEAT;
import static com.tenten.studybadge.common.constant.NotificationConstant.SCHEDULE_UPDATE_FOR_SINGLE_TO_SINGLE;
import static com.tenten.studybadge.common.constant.NotificationConstant.SINGLE_SCHEDULE_CREATE;
import static com.tenten.studybadge.common.constant.NotificationConstant.SINGLE_SCHEDULE_DELETE;

import com.tenten.studybadge.common.exception.schedule.CanNotDeleteForBeforeDateException;
import com.tenten.studybadge.common.exception.schedule.IllegalArgumentForRepeatScheduleEditRequestException;
import com.tenten.studybadge.common.exception.schedule.IllegalArgumentForRepeatSituationException;
import com.tenten.studybadge.common.exception.schedule.IllegalArgumentForScheduleEditRequestException;
import com.tenten.studybadge.common.exception.schedule.IllegalArgumentForScheduleRequestException;
import com.tenten.studybadge.common.exception.schedule.InvalidScheduleModificationException;
import com.tenten.studybadge.common.exception.schedule.NotEqualSingleScheduleDate;
import com.tenten.studybadge.common.exception.schedule.NotFoundRepeatScheduleException;
import com.tenten.studybadge.common.exception.schedule.NotFoundSingleScheduleException;
import com.tenten.studybadge.common.exception.schedule.NotIncludedInRepeatScheduleException;
import com.tenten.studybadge.common.exception.studychannel.NotFoundStudyChannelException;
import com.tenten.studybadge.common.exception.studychannel.NotStudyLeaderException;
import com.tenten.studybadge.common.exception.studychannel.NotStudyMemberException;
import com.tenten.studybadge.notification.service.NotificationSchedulerService;
import com.tenten.studybadge.notification.service.NotificationService;
import com.tenten.studybadge.schedule.domain.entity.RepeatSchedule;
import com.tenten.studybadge.schedule.domain.entity.SingleSchedule;
import com.tenten.studybadge.schedule.domain.repository.RepeatScheduleRepository;
import com.tenten.studybadge.schedule.domain.repository.SingleScheduleRepository;
import com.tenten.studybadge.schedule.dto.RepeatScheduleCreateRequest;
import com.tenten.studybadge.schedule.dto.RepeatScheduleEditRequest;
import com.tenten.studybadge.schedule.dto.ScheduleDeleteRequest;
import com.tenten.studybadge.schedule.dto.ScheduleEditRequest;
import com.tenten.studybadge.schedule.dto.ScheduleResponse;
import com.tenten.studybadge.schedule.dto.SingleScheduleCreateRequest;
import com.tenten.studybadge.schedule.dto.SingleScheduleEditRequest;
import com.tenten.studybadge.study.channel.domain.entity.StudyChannel;
import com.tenten.studybadge.study.channel.domain.repository.StudyChannelRepository;
import com.tenten.studybadge.study.member.domain.entity.StudyMember;
import com.tenten.studybadge.study.member.domain.repository.StudyMemberRepository;
import com.tenten.studybadge.type.notification.NotificationType;
import com.tenten.studybadge.type.schedule.RepeatCycle;
import com.tenten.studybadge.type.schedule.RepeatSituation;
import com.tenten.studybadge.type.schedule.ScheduleType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final SingleScheduleRepository singleScheduleRepository;
    private final RepeatScheduleRepository repeatScheduleRepository;
    private final StudyChannelRepository studyChannelRepository;
    private final StudyMemberRepository studyMemberRepository;

    private final NotificationService notificationService;
    private final NotificationSchedulerService notificationSchedulerService;

    public void postSingleSchedule(
        Long studyChannelId, SingleScheduleCreateRequest singleScheduleCreateRequest) {

        StudyChannel studyChannel = validateStudyChannel(studyChannelId);

        validateStudyLeader(singleScheduleCreateRequest.getMemberId(), studyChannelId);

        SingleSchedule saveSingleSchedule = singleScheduleRepository.save(createSingleScheduleFromRequest(
            singleScheduleCreateRequest, studyChannel));

        // 생성된 단일 일정 스케줄링 등록
        notificationSchedulerService.schedulingSingleScheduleNotification(saveSingleSchedule);
        sendNotificationForSchedule(studyChannel, saveSingleSchedule.getScheduleDate(),
            NotificationType.SCHEDULE_CREATE,
            SCHEDULE_RELATED_URL, SINGLE_SCHEDULE_CREATE);
    }

    public void postRepeatSchedule(
        Long studyChannelId, RepeatScheduleCreateRequest repeatScheduleCreateRequest) {

        StudyChannel studyChannel = validateStudyChannel(studyChannelId);

        RepeatCycle repeatCycle = repeatScheduleCreateRequest.getRepeatCycle();
        LocalDate scheduleDate = repeatScheduleCreateRequest.getScheduleDate();
        RepeatSituation repeatSituation = repeatScheduleCreateRequest.getRepeatSituation();
        validateRepeatSituation(scheduleDate, repeatCycle, repeatSituation);

        validateStudyLeader(repeatScheduleCreateRequest.getMemberId(), studyChannelId);

        RepeatSchedule saveRepeatSchedule = repeatScheduleRepository.save(
            createRepeatScheduleFromRequest(repeatScheduleCreateRequest, studyChannel));

        // 생성된 반복 일정 스케줄링 등록
        notificationSchedulerService.schedulingRepeatScheduleNotification(saveRepeatSchedule);
        sendNotificationForSchedule(studyChannel, saveRepeatSchedule.getScheduleDate(),
            NotificationType.SCHEDULE_CREATE,
            SCHEDULE_RELATED_URL, REPEAT_SCHEDULE_CREATE);
    }

    public List<ScheduleResponse> getSchedulesInStudyChannel(
        Long memberId, Long studyChannelId) {

        validateStudyChannel(studyChannelId);
        validateStudyMember(memberId, studyChannelId);

        List<ScheduleResponse> scheduleResponses = new ArrayList<>();
        List<ScheduleResponse> singleScheduleResponses = singleScheduleRepository.findAllByStudyChannelId(
                studyChannelId)
            .stream()
            .map(SingleSchedule::toResponse)
            .collect(Collectors.toList());

        List<ScheduleResponse> repeatScheduleResponses = repeatScheduleRepository.findAllByStudyChannelId(
                studyChannelId)
            .stream()
            .map(RepeatSchedule::toResponse)
            .collect(Collectors.toList());

        scheduleResponses.addAll(singleScheduleResponses);
        scheduleResponses.addAll(repeatScheduleResponses);

        return scheduleResponses;
    }

    public List<ScheduleResponse> getSchedulesInStudyChannelForYearAndMonth(
        Long memberId, Long studyChannelId, int year, int month) {
        validateStudyChannel(studyChannelId);
        validateStudyMember(memberId, studyChannelId);

        List<ScheduleResponse> scheduleResponses = new ArrayList<>();

        LocalDate selectMonthFirstDate = LocalDate.of(year, month, 1);
        LocalDate selectMonthLastDate = selectMonthFirstDate.withDayOfMonth(selectMonthFirstDate.lengthOfMonth());
        List<ScheduleResponse> singleScheduleResponses = singleScheduleRepository.findAllByStudyChannelIdAndDateRange(
                studyChannelId,selectMonthFirstDate, selectMonthLastDate)
            .stream()
            .map(SingleSchedule::toResponse)
            .collect(Collectors.toList());

        List<ScheduleResponse> repeatScheduleResponses = repeatScheduleRepository.findAllByStudyChannelIdAndDate(
                studyChannelId, selectMonthFirstDate)
            .stream()
            .map(RepeatSchedule::toResponse)
            .collect(Collectors.toList());

        scheduleResponses.addAll(singleScheduleResponses);
        scheduleResponses.addAll(repeatScheduleResponses);
        return scheduleResponses;
    }

    public SingleSchedule getSingleSchedule(
        Long memberId, Long studyChannelId, Long scheduleId) {

        studyMemberRepository.findByMemberIdAndStudyChannelId(memberId, studyChannelId)
            .orElseThrow(NotStudyMemberException::new);

        return singleScheduleRepository.findById(scheduleId)
            .orElseThrow(NotFoundSingleScheduleException::new);
    }

    public RepeatSchedule getRepeatSchedule(
        Long memberId, Long studyChannelId, Long scheduleId) {

        studyMemberRepository.findByMemberIdAndStudyChannelId(memberId, studyChannelId)
            .orElseThrow(NotStudyMemberException::new);

        return repeatScheduleRepository.findById(scheduleId)
            .orElseThrow(NotFoundSingleScheduleException::new);
    }

    public void putSchedule(
        Long studyChannelId, ScheduleEditRequest scheduleEditRequest) {
        validateStudyChannel(studyChannelId);

        if (scheduleEditRequest instanceof SingleScheduleEditRequest) {
            SingleScheduleEditRequest editRequestToSingleSchedule =
                (SingleScheduleEditRequest) scheduleEditRequest;

            if(editRequestToSingleSchedule.getOriginType() != ScheduleType.SINGLE) {
                throw new IllegalArgumentForScheduleEditRequestException();
            }
            validateStudyLeader(editRequestToSingleSchedule.getMemberId(), studyChannelId);
            putScheduleSingleToSingle(editRequestToSingleSchedule);

        } else if (scheduleEditRequest instanceof RepeatScheduleEditRequest) {
            RepeatScheduleEditRequest editRequestToRepeatSchedule =
                (RepeatScheduleEditRequest) scheduleEditRequest;
            validateStudyLeader(editRequestToRepeatSchedule.getMemberId(), studyChannelId);

            if (editRequestToRepeatSchedule.getOriginType() == ScheduleType.SINGLE) {
                putScheduleSingleToRepeat(editRequestToRepeatSchedule);
            } else if (editRequestToRepeatSchedule.getOriginType() == ScheduleType.REPEAT) {
                putScheduleRepeatToRepeat(editRequestToRepeatSchedule);
            } else {
                throw new IllegalArgumentForScheduleEditRequestException();
            }

        } else {
            throw new IllegalArgumentForScheduleEditRequestException();
        }
    }

    public void putScheduleSingleToSingle(
        SingleScheduleEditRequest editRequestToSingleSchedule) {

        SingleSchedule singleSchedule = singleScheduleRepository.findById(
                editRequestToSingleSchedule.getScheduleId())
            .orElseThrow(NotFoundSingleScheduleException::new);

        singleSchedule.updateSingleSchedule(editRequestToSingleSchedule);
        SingleSchedule updateSingleSchedule = singleScheduleRepository.save(singleSchedule);

        // 기존 단일 일정 스케줄링 삭제 & 업데이트 된 단일 일정 스케줄링 등록
        notificationSchedulerService.reschedulingSingleScheduleNotification(
            singleSchedule, updateSingleSchedule);
        sendNotificationForSchedule(singleSchedule.getStudyChannel(), singleSchedule.getScheduleDate(),
            NotificationType.SCHEDULE_UPDATE,
            SCHEDULE_RELATED_URL, SCHEDULE_UPDATE_FOR_SINGLE_TO_SINGLE);
    }

    public void putScheduleSingleToRepeat(
        RepeatScheduleEditRequest editRequestToRepeatSchedule) {

        SingleSchedule singleSchedule = singleScheduleRepository.findById(
                editRequestToRepeatSchedule.getScheduleId())
            .orElseThrow(NotFoundSingleScheduleException::new);

        LocalDate selectedDate = editRequestToRepeatSchedule.getSelectedDate();
        RepeatCycle repeatCycle = editRequestToRepeatSchedule.getRepeatCycle();
        RepeatSituation repeatSituation = editRequestToRepeatSchedule.getRepeatSituation();
        validateRepeatSituation(selectedDate, repeatCycle, repeatSituation);

        RepeatSchedule repeatSchedule = repeatScheduleRepository.save(createRepeatScheduleFromRequest(
            editRequestToRepeatSchedule, singleSchedule.getStudyChannel()));

        singleScheduleRepository.deleteById(editRequestToRepeatSchedule.getScheduleId());

        // 기존 스케줄링 해놨던 단일 일정 삭제 & 새롭게 수정한 반복 일정으로 스케줄링
        notificationSchedulerService.unSchedulingSingleScheduleNotification(singleSchedule);
        notificationSchedulerService.schedulingRepeatScheduleNotification(repeatSchedule);
        sendNotificationForSchedule(singleSchedule.getStudyChannel(), singleSchedule.getScheduleDate(),
            NotificationType.SCHEDULE_UPDATE,
            SCHEDULE_RELATED_URL, SCHEDULE_UPDATE_FOR_SINGLE_TO_REPEAT);
    }

    public void putScheduleRepeatToRepeat(
        RepeatScheduleEditRequest editRequestToRepeatSchedule) {

        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        RepeatSchedule repeatSchedule = repeatScheduleRepository.findById(
                editRequestToRepeatSchedule.getScheduleId())
            .orElseThrow(NotFoundRepeatScheduleException::new);

        if (editRequestToRepeatSchedule.getRepeatCycle() != repeatSchedule.getRepeatCycle()) {
            throw new IllegalArgumentForRepeatScheduleEditRequestException();
        }

        if(!repeatSchedule.getRepeatEndDate().isEqual(editRequestToRepeatSchedule.getRepeatEndDate())) {
            editRequestToRepeatSchedule.setRepeatEndDate(repeatSchedule.getRepeatEndDate());
        }

        LocalDate selectedDate = editRequestToRepeatSchedule.getSelectedDate();
        RepeatCycle repeatCycle = editRequestToRepeatSchedule.getRepeatCycle();
        RepeatSituation repeatSituation = editRequestToRepeatSchedule.getRepeatSituation();
        validateRepeatSituation(selectedDate, repeatCycle, repeatSituation);

        if (currentDate.isEqual(editRequestToRepeatSchedule.getSelectedDate())) {
            // 기존 시작 일정이 현재 시간보다 이전이면 이미 출석처리가 됐을 것. 그러므로 변경 불가
            validateNotPastTime(repeatSchedule.getScheduleStartTime(), currentTime);
            // 당일 일정을 변경시에 현재 시간 보다 이전으로 시작 시간으로 변경하는 것은 안됨
            validateNotPastTime(editRequestToRepeatSchedule.getScheduleStartTime(), currentTime);
        }

        repeatSchedule.updateRepeatSchedule(editRequestToRepeatSchedule);
        RepeatSchedule updateRepeatSchedule = repeatScheduleRepository.save(repeatSchedule);

        // 기존 반복 일정 스케줄링 삭제 & 업데이트 된 반복 일정 스케줄링 등록
        notificationSchedulerService.reSchedulingRepeatScheduleNotification(
            repeatSchedule, updateRepeatSchedule);
        sendNotificationForSchedule(repeatSchedule.getStudyChannel(), repeatSchedule.getScheduleDate(),
            NotificationType.SCHEDULE_UPDATE,
            SCHEDULE_RELATED_URL, SCHEDULE_UPDATE_FOR_REPEAT_TO_REPEAT);
    }

    public void putScheduleRepeatToSingle(
        Long studyChannelId, Boolean isAfterEventSame,
        SingleScheduleEditRequest editRequestToSingleSchedule) {

        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        StudyChannel studyChannel = validateStudyChannel(studyChannelId);

        RepeatSchedule repeatSchedule = repeatScheduleRepository.findById(
                editRequestToSingleSchedule.getScheduleId())
            .orElseThrow(NotFoundRepeatScheduleException::new);

        LocalDate selectedDate = editRequestToSingleSchedule.getSelectedDate();
        if (isNotIncludedRepeatSchedule(
            repeatSchedule.getRepeatCycle(), selectedDate,
            repeatSchedule.getScheduleDate(), repeatSchedule.getRepeatEndDate())) {
            throw new NotIncludedInRepeatScheduleException();
        }

        if (currentDate.isEqual(selectedDate)) {
            // 기존 시작 일정이 현재 시간보다 이전이면 이미 출석처리가 됐을 것. 그러므로 변경 불가
            validateNotPastTime(repeatSchedule.getScheduleStartTime(), currentTime);
            // 당일 일정을 변경시에 현재 시간 보다 이전으로 시작 시간으로 변경하는 것은 안됨
            validateNotPastTime(editRequestToSingleSchedule.getScheduleStartTime(), currentTime);
        }

        validateStudyLeader(editRequestToSingleSchedule.getMemberId(), studyChannelId);

        if (!isAfterEventSame) {
            putScheduleRepeatToSingleAfterEventNo(repeatSchedule, editRequestToSingleSchedule);

        } else if (isAfterEventSame) {
            putScheduleRepeatToSingleAfterEventYes(repeatSchedule, editRequestToSingleSchedule);

        } else {
            throw new IllegalArgumentForScheduleRequestException();
        }

        sendNotificationForSchedule(studyChannel, repeatSchedule.getScheduleDate(),
            NotificationType.SCHEDULE_UPDATE,
            SCHEDULE_RELATED_URL, SCHEDULE_UPDATE_FOR_REPEAT_TO_SINGLE);
    }

    public void putScheduleRepeatToSingleAfterEventYes(
        RepeatSchedule repeatSchedule, SingleScheduleEditRequest singleScheduleEditRequest) {

        LocalDate selectedDate = singleScheduleEditRequest.getSelectedDate();
        if (selectedDate.equals(repeatSchedule.getScheduleDate())) {
            repeatScheduleRepository.deleteById(singleScheduleEditRequest.getScheduleId());
            // 기존 반복 일정 삭제되었으므로 스케줄링도 삭제
            notificationSchedulerService.unSchedulingRepeatScheduleNotification(repeatSchedule);
        } else if (isNextRepeatStartDate(selectedDate, repeatSchedule.getRepeatCycle(), repeatSchedule.getScheduleDate())) {
            repeatScheduleRepository.deleteById(singleScheduleEditRequest.getScheduleId());
            // 기존 반복 일정 삭제되었으므로 스케줄링도 삭제
            notificationSchedulerService.unSchedulingRepeatScheduleNotification(repeatSchedule);
            // 새로운 단일 일정이 생겼으므로 단일 일정 스케줄링 등록
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getScheduleDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
        } else {
            // 기존 반복 일정의 수정으로 리스케줄링
            RepeatSchedule newRepeatSchedule = changeRepeatEndDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        }

        // 만일 변경한 기존 반복 일정이 반복 시작 날짜와 끝나는 날짜가 같을 경우 단일 일정으로 변경한다.
        if (repeatSchedule.getScheduleDate().equals(repeatSchedule.getRepeatEndDate())) {
            // 새로운 단일 일정 생성 -> 스케줄링 등록
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getScheduleDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
            // 기존 반복 일정 삭제되었으므로 스케줄링도 삭제
            repeatScheduleRepository.deleteById(singleScheduleEditRequest.getScheduleId());
            notificationSchedulerService.unSchedulingRepeatScheduleNotification(repeatSchedule);

        }

        // 선택 날짜 single schedule
        SingleSchedule singleSchedule = singleScheduleRepository.save(
            createSingleScheduleFromRequest(
                singleScheduleEditRequest, repeatSchedule.getStudyChannel()));
        notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
    }

    public void putScheduleRepeatToSingleAfterEventNo(
        RepeatSchedule repeatSchedule, SingleScheduleEditRequest singleScheduleEditRequest) {

        LocalDate selectedDate = singleScheduleEditRequest.getSelectedDate();
        if (selectedDate.isEqual(repeatSchedule.getScheduleDate())) {
            // 기존 반복 일정: scheduleDate = scheduleDate + (주기 1)으로 변경
            RepeatSchedule newRepeatSchedule = changeRepeatStartDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        } else if (selectedDate.isEqual(repeatSchedule.getRepeatEndDate())) {
            // 기존 반복 일정: endDate = endDate - (주기 1)으로 변경
            RepeatSchedule newRepeatSchedule = changeRepeatEndDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        } else if (isNextRepeatStartDate(selectedDate, repeatSchedule.getRepeatCycle(), repeatSchedule.getScheduleDate())) {
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getScheduleDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
            // 기존 반복 일정이 수정되서 리스케줄링
            RepeatSchedule newRepeatSchedule = changeRepeatStartDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);

        } else if (isFrontRepeatEndDate(selectedDate, repeatSchedule.getRepeatCycle(), repeatSchedule.getRepeatEndDate())) {
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getRepeatEndDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
            // 기존 반복 일정이 수정되서 리스케줄링
            RepeatSchedule newRepeatSchedule = changeRepeatEndDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        } else {
            RepeatSchedule secondRepeatSchedule =  makeAfterCycleRepeatSchedule(selectedDate, repeatSchedule);
            repeatScheduleRepository.save(secondRepeatSchedule);
            notificationSchedulerService.schedulingRepeatScheduleNotification(secondRepeatSchedule);
            // 기존 반복 일정이 수정되서 리스케줄링
            RepeatSchedule newRepeatSchedule = changeRepeatEndDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        }

        // 만일 변경한 기존 반복 일정이 반복 시작 날짜와 끝나는 날짜가 같을 경우 단일 일정으로 변경한다.
        if (repeatSchedule.getScheduleDate().isEqual(repeatSchedule.getRepeatEndDate())) {
              SingleSchedule singleSchedule = singleScheduleRepository.save(
                  createSingleScheduleFromRepeat(
                      repeatSchedule, repeatSchedule.getScheduleDate()));
              notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
              repeatScheduleRepository.deleteById(singleScheduleEditRequest.getScheduleId());
              notificationSchedulerService.unSchedulingRepeatScheduleNotification(repeatSchedule);
        }

            // 선택 날짜 single schedule
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRequest(
                    singleScheduleEditRequest, repeatSchedule.getStudyChannel()));
        notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
    }

    public void deleteSingleSchedule(
        Long studyChannelId, ScheduleDeleteRequest scheduleDeleteRequest) {
        LocalDate currentDate =LocalDate.now();

        StudyChannel studyChannel = validateStudyChannel(studyChannelId);

        SingleSchedule singleSchedule = singleScheduleRepository.findById(
                scheduleDeleteRequest.getScheduleId())
            .orElseThrow(NotFoundSingleScheduleException::new);

        if (!scheduleDeleteRequest.getSelectedDate().isEqual(singleSchedule.getScheduleDate())) {
            throw new NotEqualSingleScheduleDate();
        }

        if (currentDate.isAfter(singleSchedule.getScheduleDate())) {
            throw new CanNotDeleteForBeforeDateException();
        }
        validateStudyLeader(scheduleDeleteRequest.getMemberId(), studyChannelId);
        // 기존 단일 일정 삭제 -> 스케줄링도 삭제
        singleScheduleRepository.deleteById(scheduleDeleteRequest.getScheduleId());
        notificationSchedulerService.unSchedulingSingleScheduleNotification(singleSchedule);
        sendNotificationForSchedule(studyChannel, scheduleDeleteRequest.getSelectedDate(),
            NotificationType.SCHEDULE_DELETE,
            SCHEDULE_RELATED_URL, SINGLE_SCHEDULE_DELETE);
    }

    public void deleteRepeatSchedule(
        Long studyChannelId, Boolean isAfterEventSame,
        ScheduleDeleteRequest scheduleDeleteRequest) {
        LocalDate currentDate =LocalDate.now();

        StudyChannel studyChannel = validateStudyChannel(studyChannelId);

        RepeatSchedule repeatSchedule = repeatScheduleRepository.findById(
                scheduleDeleteRequest.getScheduleId())
            .orElseThrow(NotFoundRepeatScheduleException::new);

        LocalDate selectedDate = scheduleDeleteRequest.getSelectedDate();

        if (isNotIncludedRepeatSchedule(
            repeatSchedule.getRepeatCycle(), selectedDate,
            repeatSchedule.getScheduleDate(), repeatSchedule.getRepeatEndDate())) {
            throw new NotIncludedInRepeatScheduleException();
        }

        if (currentDate.isAfter(scheduleDeleteRequest.getSelectedDate())) {
            throw new CanNotDeleteForBeforeDateException();
        }

        validateStudyLeader(scheduleDeleteRequest.getMemberId(), studyChannelId);

        if (isAfterEventSame) {
            deleteRepeatScheduleAfterEventSameYes(selectedDate, repeatSchedule);
        } else if (!isAfterEventSame) {
            deleteRepeatScheduleAfterEventSameNo(selectedDate, repeatSchedule);
        }

        sendNotificationForSchedule(studyChannel, scheduleDeleteRequest.getSelectedDate(),
            NotificationType.SCHEDULE_DELETE,
            SCHEDULE_RELATED_URL, REPEAT_SCHEDULE_DELETE);
    }

    public void deleteRepeatScheduleAfterEventSameYes(
        LocalDate selectedDate, RepeatSchedule repeatSchedule) {

        if (selectedDate.equals(repeatSchedule.getScheduleDate())) {
            // 선택 날짜 repeat schedule 삭제
            repeatScheduleRepository.deleteById(repeatSchedule.getId());
            notificationSchedulerService.unSchedulingRepeatScheduleNotification(repeatSchedule);
        } else if (isNextRepeatStartDate(selectedDate, repeatSchedule.getRepeatCycle(), repeatSchedule.getScheduleDate())) {
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getScheduleDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
            // 기존 반복 일정 삭제
            repeatScheduleRepository.deleteById(repeatSchedule.getId());
            notificationSchedulerService.unSchedulingRepeatScheduleNotification(repeatSchedule);
        } else {
            // 기존 반복 일정 -> 수정된 반복 일정 리스케줄링
            RepeatSchedule newRepeatSchedule = changeRepeatEndDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        }
        // 만일 변경한 기존 반복 일정이 반복 시작 날짜와 끝나는 날짜가 같을 경우 단일 일정으로 변경한다.
        if (repeatSchedule.getScheduleDate().equals(repeatSchedule.getRepeatEndDate())) {
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getScheduleDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
            // 기존 반복 일정 삭제
            repeatScheduleRepository.deleteById(repeatSchedule.getId());
            notificationSchedulerService.unSchedulingRepeatScheduleNotification(repeatSchedule);
        }
    }

    public void deleteRepeatScheduleAfterEventSameNo(
        LocalDate selectedDate, RepeatSchedule repeatSchedule) {

        if (selectedDate.isEqual(repeatSchedule.getScheduleDate())) {
            // 기존 반복 일정: scheduleDate = scheduleDate + (주기 1)으로 변경
            RepeatSchedule newRepeatSchedule = changeRepeatStartDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        } else if (selectedDate.equals(repeatSchedule.getRepeatEndDate())) {
            // 기존 반복 일정: endDate = endDate - (주기 1)으로 변경
            RepeatSchedule newRepeatSchedule = changeRepeatEndDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        } else if (isNextRepeatStartDate(selectedDate, repeatSchedule.getRepeatCycle(), repeatSchedule.getScheduleDate())) {
            // 새로운 단일 일정 생성
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getScheduleDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
            // 기존 반복 일정 -> 수정된 반복 일정 리스케줄링
            RepeatSchedule newRepeatSchedule = changeRepeatStartDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);

        } else if (isFrontRepeatEndDate(selectedDate,
            repeatSchedule.getRepeatCycle(), repeatSchedule.getRepeatEndDate())) {
            // 새로운 단일 일정 생성
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getRepeatEndDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
            // 기존 반복 일정 -> 수정된 반복 일정 리스케줄링
            RepeatSchedule newRepeatSchedule = changeRepeatEndDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);

        } else {
            RepeatSchedule secondRepeatSchedule = makeAfterCycleRepeatSchedule(selectedDate,  repeatSchedule);
            repeatScheduleRepository.save(secondRepeatSchedule);
            // 새로 생긴 반복 일정 스케줄링
            notificationSchedulerService.schedulingRepeatScheduleNotification(secondRepeatSchedule);
            // 기존 반복 일정 -> 수정된 반복 일정 리스케줄링
            RepeatSchedule newRepeatSchedule = changeRepeatEndDate(selectedDate,
                repeatSchedule.getRepeatCycle(), repeatSchedule);
            notificationSchedulerService.reSchedulingRepeatScheduleNotification(
                repeatSchedule, newRepeatSchedule);
        }

        // 만일 변경한 기존 반복 일정이 반복 시작 날짜와 끝나는 날짜가 같을 경우 단일 일정으로 변경한다.
        if (repeatSchedule.getScheduleDate().isEqual(repeatSchedule.getRepeatEndDate())) {
            SingleSchedule singleSchedule = singleScheduleRepository.save(
                createSingleScheduleFromRepeat(
                    repeatSchedule, repeatSchedule.getScheduleDate()));
            notificationSchedulerService.schedulingSingleScheduleNotification(singleSchedule);
            // 기존 반복 일정 삭제
            repeatScheduleRepository.deleteById(repeatSchedule.getId());
            notificationSchedulerService.unSchedulingRepeatScheduleNotification(repeatSchedule);
        }
    }

    private StudyChannel validateStudyChannel(Long studyChannelId){
        return studyChannelRepository.findById(studyChannelId)
            .orElseThrow(NotFoundStudyChannelException::new);
    }

    private void validateStudyMember(
        Long memberId, Long studyChannelId){
        studyMemberRepository.findByMemberIdAndStudyChannelId(memberId, studyChannelId)
            .orElseThrow(NotStudyMemberException::new);
    }

    private void validateStudyLeader(
        Long memberId, Long studyChannelId) {
        StudyMember studyMember = studyMemberRepository.findByMemberIdAndStudyChannelId(memberId,
                studyChannelId)
            .orElseThrow(NotStudyMemberException::new);

        if (!studyMember.isLeader()) {
            throw new NotStudyLeaderException();
        }
    }

    private boolean isNotIncludedRepeatSchedule(
        RepeatCycle repeatCycle, LocalDate selectedDate,
        LocalDate repeatStartDate, LocalDate repeatEndDate) {
        if (selectedDate.isAfter(repeatEndDate) || selectedDate.isBefore(repeatStartDate)) {
            return true;
        }

        return switch (repeatCycle) {
            case DAILY -> false;
            case WEEKLY -> !isDateInWeeklyCycle(selectedDate, repeatStartDate);
            case MONTHLY -> !isDateInMonthlyCycle(selectedDate, repeatStartDate);
        };
    }

    private boolean isDateInWeeklyCycle(LocalDate selectedDate, LocalDate repeatStartDate) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(repeatStartDate, selectedDate);
        return daysBetween % 7 == 0;
    }

    private boolean isDateInMonthlyCycle(LocalDate selectedDate, LocalDate repeatStartDate) {
        return selectedDate.getDayOfMonth() == repeatStartDate.getDayOfMonth();
    }

    private boolean isNextRepeatStartDate(
        LocalDate selectedDate, RepeatCycle repeatCycle, LocalDate repeatStartDate) {
        return switch (repeatCycle) {
            case DAILY -> selectedDate.minusDays(1).isEqual(repeatStartDate);
            case WEEKLY -> selectedDate.minusWeeks(1).isEqual(repeatStartDate);
            case MONTHLY -> selectedDate.minusMonths(1).isEqual(repeatStartDate);
        };
    }

    private boolean isFrontRepeatEndDate(
        LocalDate selectedDate, RepeatCycle repeatCycle, LocalDate repeatEndDate) {
        return switch (repeatCycle) {
            case DAILY -> selectedDate.plusDays(1).isEqual(repeatEndDate);
            case WEEKLY -> selectedDate.plusWeeks(1).isEqual(repeatEndDate);
            case MONTHLY -> selectedDate.plusMonths(1).isEqual(repeatEndDate);
        };
    }

    private RepeatSchedule changeRepeatStartDate(
        LocalDate selectedDate, RepeatCycle repeatCycle, RepeatSchedule repeatSchedule) {

        LocalDate newStartDate = switch (repeatCycle) {
            case DAILY -> selectedDate.plusDays(1);
            case WEEKLY -> selectedDate.plusWeeks(1);
            case MONTHLY -> selectedDate.plusMonths(1);
        };
        repeatSchedule.setRepeatStartDate(newStartDate);
        return repeatScheduleRepository.save(repeatSchedule);
    }

    private RepeatSchedule changeRepeatEndDate(
        LocalDate selectedDate, RepeatCycle repeatCycle, RepeatSchedule repeatSchedule) {
        LocalDate newEndDate = switch (repeatCycle) {
            case DAILY -> selectedDate.minusDays(1);
            case WEEKLY -> selectedDate.minusWeeks(1);
            case MONTHLY -> selectedDate.minusMonths(1);
        };
        repeatSchedule.setRepeatEndDate(newEndDate);
        return repeatScheduleRepository.save(repeatSchedule);
    }

    private RepeatSchedule makeAfterCycleRepeatSchedule(
        LocalDate selectedDate, RepeatSchedule existRepeatSchedule) {
        LocalDate afterStartDate = switch (existRepeatSchedule.getRepeatCycle()) {
            case DAILY -> selectedDate.plusDays(1);
            case WEEKLY -> selectedDate.plusWeeks(1);
            case MONTHLY -> selectedDate.plusMonths(1);
        };

        return  RepeatSchedule.withoutIdBuilder()
            .scheduleName(existRepeatSchedule.getScheduleName())
            .scheduleContent(existRepeatSchedule.getScheduleContent())
            .scheduleDate(afterStartDate)
            .scheduleStartTime(existRepeatSchedule.getScheduleStartTime())
            .scheduleEndTime(existRepeatSchedule.getScheduleEndTime())
            .isRepeated(true)
            .studyChannel(existRepeatSchedule.getStudyChannel())
            .repeatSituation(existRepeatSchedule.getRepeatSituation())
            .repeatCycle(existRepeatSchedule.getRepeatCycle())
            .repeatEndDate(existRepeatSchedule.getRepeatEndDate())
            .build();
    }

    private void validateNotPastTime(
        LocalTime scheduleStartTime, LocalTime currentTime) {
        if (scheduleStartTime.isBefore(currentTime)) {
            throw new InvalidScheduleModificationException("당일의 일정을 변경할 경우 일정의 시작 시간 이전만 가능합니다.");
        }
    }

    private void validateRepeatSituation(
        LocalDate scheduleDate, RepeatCycle repeatCycle, RepeatSituation repeatSituation) {
        if (!isValidRepeatSituation(scheduleDate, repeatCycle, repeatSituation)) {
            throw new IllegalArgumentForRepeatSituationException();
        }
    }

    private boolean isValidRepeatSituation(
        LocalDate scheduleDate, RepeatCycle repeatCycle, RepeatSituation repeatSituation) {
        switch (repeatCycle) {
            case DAILY:
                return true; // DAILY 주기에서는 특별한 검증이 필요하지 않으므로 통과
            case WEEKLY:
                String name = scheduleDate.getDayOfWeek().name();
                return repeatSituation.name().equals(scheduleDate.getDayOfWeek().name());
            case MONTHLY:
                return (Integer) repeatSituation.getDescription() == scheduleDate.getDayOfMonth();
            default:
                throw new IllegalArgumentForScheduleRequestException();
        }
    }

    private RepeatSchedule createRepeatScheduleFromRequest(
        RepeatScheduleCreateRequest repeatScheduleCreateRequest, StudyChannel studyChannel) {
        return RepeatSchedule.withoutIdBuilder()
            .scheduleName(repeatScheduleCreateRequest.getScheduleName())
            .scheduleContent(repeatScheduleCreateRequest.getScheduleContent())
            .scheduleDate(repeatScheduleCreateRequest.getScheduleDate())
            .scheduleStartTime(repeatScheduleCreateRequest.getScheduleStartTime())
            .scheduleEndTime(repeatScheduleCreateRequest.getScheduleEndTime())
            .isRepeated(true)
            .repeatCycle(repeatScheduleCreateRequest.getRepeatCycle())
            .repeatSituation(repeatScheduleCreateRequest.getRepeatSituation())
            .repeatEndDate(repeatScheduleCreateRequest.getRepeatEndDate())
            .studyChannel(studyChannel)
            .placeId(repeatScheduleCreateRequest.getPlaceId())
            .build();
    }
    private RepeatSchedule createRepeatScheduleFromRequest(
        RepeatScheduleEditRequest editRequestToRepeatSchedule, StudyChannel studyChannel) {
        return RepeatSchedule.withoutIdBuilder()
            .scheduleName(editRequestToRepeatSchedule.getScheduleName())
            .scheduleContent(editRequestToRepeatSchedule.getScheduleContent())
            .scheduleContent(editRequestToRepeatSchedule.getScheduleContent())
            .scheduleDate(editRequestToRepeatSchedule.getSelectedDate())
            .scheduleStartTime(editRequestToRepeatSchedule.getScheduleStartTime())
            .scheduleEndTime(editRequestToRepeatSchedule.getScheduleEndTime())
            .isRepeated(true)
            .repeatEndDate(editRequestToRepeatSchedule.getRepeatEndDate())
            .repeatCycle(editRequestToRepeatSchedule.getRepeatCycle())
            .repeatSituation(editRequestToRepeatSchedule.getRepeatSituation())
            .studyChannel(studyChannel)
            .placeId(editRequestToRepeatSchedule.getPlaceId())
            .build();
    }

    private SingleSchedule createSingleScheduleFromRepeat(
        RepeatSchedule repeatSchedule, LocalDate repeatEndDate) {
        return SingleSchedule.withoutIdBuilder()
            .scheduleName(repeatSchedule.getScheduleName())
            .scheduleContent(repeatSchedule.getScheduleContent())
            .scheduleDate(repeatEndDate)
            .scheduleStartTime(repeatSchedule.getScheduleStartTime())
            .scheduleEndTime(repeatSchedule.getScheduleEndTime())
            .studyChannel(repeatSchedule.getStudyChannel())
            .placeId(repeatSchedule.getPlaceId())
            .isRepeated(false)
            .build();
    }

    private SingleSchedule createSingleScheduleFromRequest(
        SingleScheduleEditRequest singleScheduleEditRequest, StudyChannel studyChannel) {
        return SingleSchedule.withoutIdBuilder()
            .scheduleName(singleScheduleEditRequest.getScheduleName())
            .scheduleContent(singleScheduleEditRequest.getScheduleContent())
            .scheduleDate(singleScheduleEditRequest.getSelectedDate())
            .scheduleStartTime(singleScheduleEditRequest.getScheduleStartTime())
            .scheduleEndTime(singleScheduleEditRequest.getScheduleEndTime())
            .studyChannel(studyChannel)
            .placeId(singleScheduleEditRequest.getPlaceId())
            .isRepeated(false)
            .build();
    }

    private SingleSchedule createSingleScheduleFromRequest(
        SingleScheduleCreateRequest singleScheduleCreateRequest, StudyChannel studyChannel) {
        return SingleSchedule.withoutIdBuilder()
            .scheduleName(singleScheduleCreateRequest.getScheduleName())
            .scheduleContent(singleScheduleCreateRequest.getScheduleContent())
            .scheduleDate(singleScheduleCreateRequest.getScheduleDate())
            .scheduleStartTime(singleScheduleCreateRequest.getScheduleStartTime())
            .scheduleEndTime(singleScheduleCreateRequest.getScheduleEndTime())
            .studyChannel(studyChannel)
            .placeId(singleScheduleCreateRequest.getPlaceId())
            .isRepeated(false)
            .build();
    }

    private void sendNotificationForSchedule(
        StudyChannel studyChannel, LocalDate scheduleDate,
        NotificationType notificationType,
        String relateUrlFormat, String notificationFormatMessage) {

        // 로그 메시지 및 알림 메시지에 사용할 날짜 포맷터
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 일정 날짜를 클라이언트 쪽 관련 url으로 포맷팅
        String formattedDate = scheduleDate.format(formatter);
        // 알림 메시지 생성
        String notificationMessage = String.format(
            notificationFormatMessage, studyChannel.getName(), formattedDate);
        // 관련 URL 생성
        String relateUrl = String.format(
            relateUrlFormat, studyChannel.getId(), formattedDate);
        notificationService.sendNotificationToStudyChannel(studyChannel.getId(),
            notificationType, notificationMessage, relateUrl);
        // 로그 메시지
        log.info(notificationMessage);
    }
}
