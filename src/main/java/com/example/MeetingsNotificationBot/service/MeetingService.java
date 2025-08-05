package com.example.MeetingsNotificationBot.service;

import com.example.MeetingsNotificationBot.entity.Meeting;
import com.example.MeetingsNotificationBot.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    public void saveMeeting(Long chatId, String title, LocalDateTime time) {
        Meeting meeting = new Meeting();
        meeting.setChatId(chatId);
        meeting.setTitle(title);
        meeting.setTime(time);
        meetingRepository.save(meeting);
    }


    public List<Meeting> getMeetings(Long chatId){
        return meetingRepository.findAllByChatId(chatId);
    }

}
