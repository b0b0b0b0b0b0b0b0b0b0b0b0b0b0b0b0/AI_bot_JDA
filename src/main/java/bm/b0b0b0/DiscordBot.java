package bm.b0b0b0;

import bm.b0b0b0.ai.ApiService;
import bm.b0b0b0.util.Config;
import bm.b0b0b0.util.MessageService;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.util.EnumSet;

public class DiscordBot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(DiscordBot.class);
    private final ApiService apiService;
    private final MessageService messageService;
    private final Config config;  // Добавляем поле для конфигурации

    public DiscordBot(Config config) {
        this.config = config;
        this.apiService = new ApiService();
        this.messageService = new MessageService();
    }


    public void start() {
        String botToken = config.getBotToken();  // Получаем токен из конфигурации
        if (botToken == null || botToken.isEmpty()) {
            logger.error("Ошибка: токен бота не указан в конфиге.");
            return;
        }

        try {
            JDA jda = JDABuilder.createDefault(botToken)
                    .addEventListeners(this)
                    .enableIntents(
                            EnumSet.of(
                                    GatewayIntent.GUILD_MEMBERS,
                                    GatewayIntent.GUILD_MESSAGES,
                                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                                    GatewayIntent.MESSAGE_CONTENT,
                                    GatewayIntent.DIRECT_MESSAGES,
                                    GatewayIntent.GUILD_PRESENCES
                            )
                    )
                    .build();
            logger.info("Бот успешно запущен.");
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка: неверный токен бота.", e);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String userId = event.getAuthor().getId();
        String userName = event.getAuthor().getName();
        String messageContent = event.getMessage().getContentRaw();
        String channelId = event.getChannel().getId();  // Получаем ID канала

        // Проверяем, является ли канал разрешённым
        if (channelId.equals(config.getChannels().getGeneral()) || channelId.equals(config.getChannels().getSupport())) {

            if (!event.getAuthor().isBot()) {
                logger.info("Получено сообщение от пользователя [ID: {}, Name: {}]: {}", userId, userName, messageContent);

                // Отправляем сообщение о начале обработки и сохраняем его для последующего редактирования
                Message initialMessage = messageService.sendInitialProcessingMessage(event.getMessage());

                // Проверяем, есть ли прикрепленные файлы
                String fileDescription = "";
                try {
                    if (!event.getMessage().getAttachments().isEmpty()) {
                        fileDescription = messageService.processFileAttachments(event.getMessage().getAttachments());
                    }
                } catch (IOException e) {
                    logger.error("Ошибка при обработке вложений: ", e);
                    messageService.sendError(event.getMessage());
                    return;  // Прерываем выполнение, если ошибка
                }

                // Запрос к AI
                String response = apiService.askAI(messageContent + fileDescription);

                if (response != null) {
                    // Отправляем ответ пользователю, редактируя предыдущее сообщение
                    messageService.handleUserRequest(event.getMessage(), response, fileDescription, initialMessage);
                    logger.info("Ответ отправлен пользователю [ID: {}, Name: {}]: {}", userId, userName, response);
                } else {
                    // Отправляем сообщение об ошибке
                    messageService.sendError(event.getMessage());
                    logger.warn("Не удалось получить ответ от API для пользователя [ID: {}, Name: {}]", userId, userName);
                }
            }
        } else {
            logger.info("Сообщение отправлено в неразрешённый канал: {}", channelId);
        }
    }



}
