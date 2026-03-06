package ua.lemmatizer;

import com.formdev.flatlaf.FlatLightLaf;
// Можна також спробувати темну тему: import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        // Встановлюємо сучасний дизайн FlatLaf
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Не вдалося ініціалізувати FlatLaf");
        }

        // Запуск вікна
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}