package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.Repository.NotificationTaskRepository;
import pro.sky.telegrambot.model.NotificationTask;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class TelegramBotUpdatesListener implements UpdatesListener {


    final private NotificationTaskRepository notificationTaskRepository;

    private final Logger logger;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    }

    private TelegramBot telegramBot;

    private final Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");


    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {

        updates.forEach(update -> {
            logger.info("Processing update: {}", update);


            if (update.message().text().startsWith("/start")) {
                SendMessage message = new SendMessage(update.message().chat().id(), "hello");
                telegramBot.execute(message);
            }
            Matcher matcher = pattern.matcher(update.message().text());
            if (matcher.matches()) {
                String date = matcher.group(1);
                String massage = matcher.group(3);
                LocalDateTime tempDate = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                NotificationTask temp = new NotificationTask();
                temp.setChatId(update.message().chat().id());
                temp.setMassage(massage);
                temp.setNotificationDate(tempDate);
                notificationTaskRepository.save(temp);
                SendMessage messageTemp = new SendMessage(update.message().chat().id(), "Notification added");
                telegramBot.execute(messageTemp);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void run() {
        List<NotificationTask> taskList = notificationTaskRepository.findAllByNotificationDateBefore(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        if (!taskList.isEmpty()) {
            for (NotificationTask task : taskList) {
                SendMessage notificationMessage = new SendMessage(
                        task.getChatId(),
                        task.getMassage());
                telegramBot.execute(notificationMessage);
            }

            for (NotificationTask notificationTask : taskList) {
                notificationTaskRepository.deleteById(notificationTask.getId());
            }

        }
    }
}