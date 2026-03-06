package ua.lemmatizer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;

public class MainWindow extends JFrame {
    private EnglishLemmatizer englishLemmatizer;
    private File selectedFile;
    private Path lastSavedDir;

    private JTextArea logArea;
    private JButton selectFileBtn;
    private JButton processBtn;
    private JButton openFolderBtn;
    private JCheckBox cleanTextCheck;
    private JComboBox<String> langCombo;

    public MainWindow() {
        setTitle("Lemmatizer App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 450);
        setLocationRelativeTo(null);

        initUI();

        log("Запуск програми... Завантаження словників у фоні (зачекайте пару секунд).");
        new Thread(() -> {
            englishLemmatizer = new EnglishLemmatizer();
            SwingUtilities.invokeLater(() -> {
                log("✅ Моделі завантажено! Програма готова до роботи.");
                processBtn.setEnabled(true);
            });
        }).start();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Вибір мови (Українська перша за замовчуванням)
        langCombo = new JComboBox<>(new String[]{"Українська", "English"});
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        langPanel.add(new JLabel("Мова тексту: "));
        langPanel.add(langCombo);
        topPanel.add(langPanel);

        cleanTextCheck = new JCheckBox("Видаляти розділові знаки", true);
        topPanel.add(cleanTextCheck);

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectFileBtn = new JButton("Вибрати .txt файл");
        JLabel fileLabel = new JLabel("Файл не вибрано");

        selectFileBtn.addActionListener(e -> {
            java.awt.FileDialog fileDialog = new java.awt.FileDialog(this, "Виберіть .txt файл", java.awt.FileDialog.LOAD);
            fileDialog.setFile("*.txt");
            fileDialog.setVisible(true);

            String dir = fileDialog.getDirectory();
            String file = fileDialog.getFile();

            if (dir != null && file != null) {
                selectedFile = new File(dir, file);
                fileLabel.setText(selectedFile.getName());
                openFolderBtn.setVisible(false);
            }
        });

        filePanel.add(selectFileBtn);
        filePanel.add(fileLabel);
        topPanel.add(filePanel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        processBtn = new JButton("Обробити та Зберегти");
        processBtn.setEnabled(false);
        processBtn.addActionListener(e -> processFile());

        openFolderBtn = new JButton("📂 Відкрити папку");
        openFolderBtn.setVisible(false);
        openFolderBtn.addActionListener(e -> openResultFolder());

        actionPanel.add(processBtn);
        actionPanel.add(openFolderBtn);
        topPanel.add(actionPanel);

        add(topPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setMargin(new Insets(10, 10, 10, 10));
        logArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        add(new JScrollPane(logArea), BorderLayout.CENTER);
    }

    private void processFile() {
        if (selectedFile == null) {
            log("❌ Помилка: Спочатку виберіть файл!");
            return;
        }

        processBtn.setEnabled(false);
        openFolderBtn.setVisible(false);
        log("⏳ Читання файлу: " + selectedFile.getName());

        new Thread(() -> {
            try {
                Path inputPath = selectedFile.toPath();
                String text = FileService.readFromFile(inputPath);
                String selectedLang = (String) langCombo.getSelectedItem();

                // 1. ПЕРЕВІРКА МОВИ перед обробкою
                if (!checkLanguageMatch(text, selectedLang)) {
                    log("🛑 Обробку скасовано користувачем (невідповідність мови).");
                    SwingUtilities.invokeLater(() -> processBtn.setEnabled(true));
                    return; // Зупиняємо виконання, якщо користувач натиснув "Ні"
                }

                // 2. Якщо все ок, йдемо далі
                if (cleanTextCheck.isSelected()) {
                    log("⚙️ Очищення тексту від пунктуації...");
                    text = TextCleaner.removePunctuation(text);
                }

                log("🧠 Лематизація (" + selectedLang + ")...");
                String result = "";

                if ("English".equals(selectedLang)) {
                    result = englishLemmatizer.lemmatize(text);
                } else if ("Українська".equals(selectedLang)) {
                    UkrainianLemmatizer ukrainianLemmatizer = new UkrainianLemmatizer();
                    result = ukrainianLemmatizer.lemmatize(text);
                }

                log("💾 Збереження результату...");
                Path outputPath = FileService.saveToFile(inputPath, result, "_done");

                lastSavedDir = outputPath.getParent();
                log("--------------------------------------------------");
                log("🎉 Успіх! Файл збережено: " + outputPath.toAbsolutePath());
                log("Або натисніть на |📂 Відкрити папку|, щоб перейти");
                log("--------------------------------------------------");

                SwingUtilities.invokeLater(() -> openFolderBtn.setVisible(true));

            } catch (Exception ex) {
                log("❌ Помилка: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> processBtn.setEnabled(true));
            }
        }).start();
    }

    // --- НОВИЙ МЕТОД ДЛЯ ПЕРЕВІРКИ МОВИ ---
    private boolean checkLanguageMatch(String text, String selectedLang) throws InterruptedException, InvocationTargetException {
        int cyrillicCount = 0;
        int latinCount = 0;

        // Перевіряємо перші 5000 символів (для швидкості)
        int limit = Math.min(text.length(), 5000);
        for (int i = 0; i < limit; i++) {
            char c = text.charAt(i);
            if ((c >= 'а' && c <= 'я') || (c >= 'А' && c <= 'Я') || "іІїЇєЄґҐ'".indexOf(c) != -1) {
                cyrillicCount++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                latinCount++;
            }
        }

        boolean isTextUkr = cyrillicCount > latinCount;
        boolean isUkrSelected = "Українська".equals(selectedLang);

        // Якщо є невідповідність, викликаємо спливаюче вікно у головному потоці (EDT)
        if (isUkrSelected && !isTextUkr && latinCount > 0) {
            return showWarningDialog("Ви обрали українську мову, але текст схожий на англійський.\nПродовжити обробку?");
        } else if (!isUkrSelected && isTextUkr && cyrillicCount > 0) {
            return showWarningDialog("Ви обрали англійську мову, але текст схожий на український.\nПродовжити обробку?");
        }

        return true; // Мова збігається, продовжуємо
    }

    // Допоміжний метод для відображення діалогу
    private boolean showWarningDialog(String message) throws InterruptedException, InvocationTargetException {
        final boolean[] userAgreed = new boolean[1];

        SwingUtilities.invokeAndWait(() -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    message,
                    "Увага: Невідповідність мови",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            userAgreed[0] = (choice == JOptionPane.YES_OPTION);
        });

        return userAgreed[0];
    }

    private void openResultFolder() {
        if (lastSavedDir != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(lastSavedDir.toFile());
            } catch (Exception ex) {
                log("❌ Не вдалося відкрити папку: " + ex.getMessage());
            }
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}