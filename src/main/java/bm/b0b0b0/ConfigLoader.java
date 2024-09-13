package bm.b0b0b0;

import bm.b0b0b0.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static Config config;
    private static final String CONFIG_FILE_PATH = "config.json";

    public static Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    private static void loadConfig() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            logger.warn("Конфиг не найден, распаковываю дефолтный из ресурсов...");
            extractDefaultConfig();
        }
        try (FileReader reader = new FileReader(CONFIG_FILE_PATH)) {
            Gson gson = new Gson();
            config = gson.fromJson(reader, Config.class);
            logger.info("Конфиг успешно загружен.");
        } catch (IOException e) {
            logger.error("Не удалось загрузить конфиг. Проверь файл.", e); // Логирование ошибки
        }
    }

    private static void extractDefaultConfig() {
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream("config.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("Дефолтный конфиг не найден в ресурсах.");
            }
            Files.copy(inputStream, Paths.get(CONFIG_FILE_PATH));
            logger.info("Дефолтный конфиг распакован в {}", CONFIG_FILE_PATH);
        } catch (IOException e) {
            logger.error("Ошибка при распаковке дефолтного конфига.", e); // Логирование ошибки
        }
    }
}
