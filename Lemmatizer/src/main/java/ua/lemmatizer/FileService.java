package ua.lemmatizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {

    public static String readFromFile(Path filePath) throws IOException {
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    public static Path saveToFile(Path originalPath, String content, String suffix) throws IOException {
        String originalName = originalPath.getFileName().toString();
        String newName = originalName.replaceFirst("[.][^.]+$", "") + suffix + ".txt";
        Path newPath = originalPath.resolveSibling(newName);

        Files.writeString(newPath, content, StandardCharsets.UTF_8);
        return newPath;
    }
}