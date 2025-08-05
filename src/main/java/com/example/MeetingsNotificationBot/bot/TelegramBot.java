package com.example.MeetingsNotificationBot.bot;


import com.example.MeetingsNotificationBot.entity.Meeting;
import com.example.MeetingsNotificationBot.service.MeetingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TelegramBot  extends TelegramLongPollingBot {

    private final String botUsername;

    public TelegramBot(@Value("${telegram.bot.username}") String botUsername,
                       @Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Autowired
    private MeetingService meetingService;

    private final Map<Long, String> state = new HashMap<>();
    private final Map<Long, String> titles = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            this.execute(new SetMyCommands(List.of(
                    new BotCommand("/add", "Add a meeting"),
                    new BotCommand("/view", "View all meetings")
            ), new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Failed to register commands", e);
        }
    }
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            if(text.equals("/start")){
                sendMessage(chatId, "Welcome! Use /add or /view.");
            }else if(text.equals("/add")){
                state.put(chatId,"MEETING_TITLE");
                sendMessage(chatId, "Enter meeting title:");
            }else if (text.equals("/view")) {
                List<Meeting> meetings = meetingService.getMeetings(chatId);
                if (meetings.isEmpty()) {
                    sendMessage(chatId, "No meetings found.");
                } else {
                    StringBuilder sb = new StringBuilder("Meetings:\n");
                    for (Meeting m : meetings) {
                        sb.append("- ").append(m.getTitle()).append(" at ").append(m.getTime()).append("\n");
                    }
                    sendMessage(chatId, sb.toString());
                }
            }else  if(state.containsKey(chatId)){
                String step = state.get(chatId);
                if ("MEETING_TITLE".equals(step)) {
                    titles.put(chatId, text);
                    state.put(chatId, "MEETING_DATE");
                    sendMessage(chatId, "Enter meeting date & time (e.g. 2025-08-05T14:30):");
                } else if ("MEETING_DATE".equals(step)) {
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(text);
                        if (dateTime.isBefore(LocalDateTime.now())) {
                            sendMessage(chatId, "Meeting time must be in the future.");
                            return;
                        }
                        meetingService.saveMeeting(chatId, titles.get(chatId), dateTime);
                        sendMessage(chatId, "Meeting saved.");
                    } catch (DateTimeParseException e) {
                        sendMessage(chatId, "Invalid format! Use: 2025-08-05T14:30");
                    }
                    state.remove(chatId);
                    titles.remove(chatId);
                }
            }
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}








