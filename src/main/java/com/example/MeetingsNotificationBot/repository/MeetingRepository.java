package com.example.MeetingsNotificationBot.repository;

import com.example.MeetingsNotificationBot.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findAllByChatId(Long chatId);
}
