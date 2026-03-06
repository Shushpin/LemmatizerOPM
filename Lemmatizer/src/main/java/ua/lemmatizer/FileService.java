package ua.lemmatizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {

    public static String readFromFile(Path filePath) throws IOException {
        return Files.readString(filePath);
    }

    public static Path saveToFile(Path originalPath, String content, String suffix) throws IOException {
        String originalName = originalPath.getFileName().toString();
        // Створюємо нове ім'я файлу
        String newName = originalName.replaceFirst("[.][^.]+$", "") + suffix + ".txt";

        // Шлях для нового файлу в тій самій папці
        Path newPath = originalPath.resolveSibling(newName);

        Files.writeString(newPath, content);
        return newPath;
    }
}
