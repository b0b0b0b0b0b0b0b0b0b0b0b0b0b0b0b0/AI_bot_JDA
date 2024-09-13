package bm.b0b0b0.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import okhttp3.*;
import org.apache.commons.io.IOUtils;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MessageService {

    private static final int MAX_EMBED_LENGTH = 4096;
    private static final List<String> allowedFileExtensions = Arrays.asList("log", "txt", "json", "properties", "yml", "png", "jpg", "jpeg");

    public void handleUserRequest(Message message, String response, String fileDescription, Message initialMessage) {
        try {
            String userMention = message.getAuthor().getAsMention();
            String userQuestion = message.getContentRaw();

            // Ограничиваем вопрос до 50 символов, но НЕ добавляем содержимое файла в вопрос
            String truncatedQuestion = userQuestion.length() > 50
                    ? userQuestion.substring(0, 50) + "..."
                    : userQuestion;

            // Включаем только краткое описание файла в ответе, но НЕ сам файл
            String responseHeader = String.format("**❔ Вопрос от %s:**\n\n%s\n\n**💡 Ответ:**\n",
                    userMention, truncatedQuestion);
            int maxResponseLength = MAX_EMBED_LENGTH - responseHeader.length();

            if (response.length() <= maxResponseLength) {
                EmbedBuilder embed = createResponseEmbed(responseHeader + response, message);
                initialMessage.editMessageEmbeds(embed.build()).queue();
            } else {
                initialMessage.delete().queue();
                String remainingResponse = response;

                while (!remainingResponse.isEmpty()) {
                    String currentResponse = remainingResponse.substring(0, Math.min(remainingResponse.length(), maxResponseLength));
                    remainingResponse = remainingResponse.substring(currentResponse.length());

                    EmbedBuilder embed = createResponseEmbed(currentResponse, message);
                    message.replyEmbeds(embed.build()).queue();
                }
            }
        } catch (Exception e) {
            sendError(message);
        }
    }


    public String processFileAttachments(List<Attachment> attachments) throws IOException {
        StringBuilder fileContentBuilder = new StringBuilder();
        for (Attachment attachment : attachments) {
            String fileExtension = getFileExtension(attachment.getFileName());
            if (allowedFileExtensions.contains(fileExtension)) {
                Response response = new OkHttpClient().newCall(new Request.Builder().url(attachment.getUrl()).build()).execute();
                assert response.body() != null;
                String fileContent = IOUtils.toString(response.body().byteStream(), StandardCharsets.UTF_8);
                fileContentBuilder.append("\n\n").append(fileContent);
            }
        }
        return !fileContentBuilder.isEmpty() ? "📎 **Взял ваш первый файл на просмотр.**\n\n" + fileContentBuilder : "";
    }

    public Message sendInitialProcessingMessage(Message message) {
        EmbedBuilder initialEmbed = new EmbedBuilder()
                .setColor(Color.YELLOW)
                .setTitle("⏳ Обработка запроса")
                .setDescription("Пожалуйста, подождите, ваш запрос обрабатывается.")
                .setFooter("Сделано с любовью к Майнкрафт 💕", message.getJDA().getSelfUser().getAvatarUrl());

        return message.replyEmbeds(initialEmbed.build()).complete();  // Отправляем сообщение и возвращаем его для редактирования
    }

    private String getFileExtension(String fileName) {
        return fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "";
    }

    public EmbedBuilder createResponseEmbed(String response, Message message) {
        return new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle("🟢 BM gpt 🟢")
                .setDescription(response)
                .setFooter("Сделано с любовью 💕", message.getJDA().getSelfUser().getAvatarUrl());
    }

    public void sendError(Message message) {
        EmbedBuilder errorEmbed = new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("❌ Ошибка")
                .setDescription("Произошла ошибка при обработке вашего запроса. Пожалуйста, попробуйте ещё раз или обратитесь к администратору.")
                .setFooter("Сделано с любовью 💕", message.getJDA().getSelfUser().getAvatarUrl());

        message.replyEmbeds(errorEmbed.build()).queue();
    }
}
