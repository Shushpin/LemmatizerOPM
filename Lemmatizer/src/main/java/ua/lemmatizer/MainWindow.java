package ua.lemmatizer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainWindow extends JFrame {
    private EnglishLemmatizer englishLemmatizer;
    // ТЕПЕР ТУТ СПИСОК ФАЙЛІВ, А НЕ ОДИН
    private List<File> selectedFiles = new ArrayList<>();
    private Path lastSavedDir;

    private JTextArea logArea;
    private JButton selectFileBtn;
    private JButton selectFolderBtn; // НОВА КНОПКА
    private JButton processBtn;
    private JButton openFolderBtn;
    private JCheckBox cleanTextCheck;
    private JComboBox<String> langCombo;
    private JProgressBar progressBar; // ПРОГРЕС-БАР

    public MainWindow() {
        setTitle("Lemmatizer App Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 500); // Зробив трохи більшим, щоб все влізло
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
        // Змінили на 5 рядків, бо додали прогрес-бар
        JPanel topPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // 1. Вибір мови
        langCombo = new JComboBox<>(new String[]{"Українська", "English"});
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        langPanel.add(new JLabel("Мова тексту: "));
        langPanel.add(langCombo);
        topPanel.add(langPanel);

        // 2. Чекбокс
        cleanTextCheck = new JCheckBox("Видаляти розділові знаки", true);
        topPanel.add(cleanTextCheck);

        // 3. Кнопки вибору Файлу / Папки
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectFileBtn = new JButton("Вибрати 1 файл");
        selectFolderBtn = new JButton("Вибрати папку");
        JLabel fileLabel = new JLabel("Нічого не вибрано");

        // Логіка для 1 файлу (через нативний FileDialog)
        selectFileBtn.addActionListener(e -> {
            java.awt.FileDialog fileDialog = new java.awt.FileDialog(this, "Виберіть .txt файл", java.awt.FileDialog.LOAD);
            fileDialog.setFile("*.txt");
            fileDialog.setVisible(true);

            String dir = fileDialog.getDirectory();
            String file = fileDialog.getFile();

            if (dir != null && file != null) {
                selectedFiles.clear(); // Очищаємо список
                selectedFiles.add(new File(dir, file)); // Додаємо 1 файл
                fileLabel.setText("Обрано: " + file);
                openFolderBtn.setVisible(false);
            }
        });

        // Логіка для ПАПКИ (через JFileChooser для сумісності з Windows)
        selectFolderBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Оберіть папку з .txt файлами");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File dir = chooser.getSelectedFile();
                selectedFiles.clear();

                // Шукаємо всі .txt файли у вибраній папці
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));
                if (files != null && files.length > 0) {
                    selectedFiles.addAll(Arrays.asList(files));
                    fileLabel.setText("Обрано папку. Знайдено файлів: " + selectedFiles.size());
                    openFolderBtn.setVisible(false);
                } else {
                    fileLabel.setText("❌ У папці немає .txt файлів!");
                }
            }
        });

        filePanel.add(selectFileBtn);
        filePanel.add(selectFolderBtn);
        filePanel.add(fileLabel);
        topPanel.add(filePanel);

        // 4. Прогрес-бар
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Очікування...");
        progressPanel.add(progressBar, BorderLayout.CENTER);
        topPanel.add(progressPanel);

        // 5. Кнопки обробки
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        processBtn = new JButton("Обробити та Зберегти");
        processBtn.setEnabled(false);
        processBtn.addActionListener(e -> processFiles()); // Викликаємо новий метод

        openFolderBtn = new JButton("📂 Відкрити папку з обробленим файлом (файлами)");
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

    private void processFiles() {
        if (selectedFiles.isEmpty()) {
            log("❌ Помилка: Спочатку виберіть файл або папку!");
            return;
        }

        processBtn.setEnabled(false);
        openFolderBtn.setVisible(false);

        progressBar.setMaximum(selectedFiles.size());
        progressBar.setValue(0);

        log("⏳ Починаємо масову обробку (" + selectedFiles.size() + " шт.)...");

        new Thread(() -> {
            long startTime = System.currentTimeMillis();

            int processedCount = 0;
            String selectedLang = (String) langCombo.getSelectedItem();
            boolean skipLanguageCheck = false;

            for (File file : selectedFiles) {
                try {
                    Path inputPath = file.toPath();
                    String text = FileService.readFromFile(inputPath);

                    if (!skipLanguageCheck && !checkLanguageMatch(text, selectedLang)) {
                        log("🛑 Скасовано: Невідповідність мови у файлі " + file.getName());
                        break;
                    }
                    skipLanguageCheck = true;

                    if (cleanTextCheck.isSelected()) {
                        text = TextCleaner.removePunctuation(text);
                    }

                    String result = "";
                    if ("English".equals(selectedLang)) {
                        result = englishLemmatizer.lemmatize(text);
                    } else if ("Українська".equals(selectedLang)) {
                        UkrainianLemmatizer ukrainianLemmatizer = new UkrainianLemmatizer();
                        result = ukrainianLemmatizer.lemmatize(text);
                    }

                    Path outputPath = FileService.saveToFile(inputPath, result, "_done");
                    lastSavedDir = outputPath.getParent();

                    log("✅ Оброблено: " + file.getName());

                } catch (Exception ex) {
                    log("❌ Помилка файлу " + file.getName() + ": " + ex.getMessage());
                } finally {
                    processedCount++;
                    final int current = processedCount;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(current);
                        progressBar.setString("Оброблено: " + current + " / " + selectedFiles.size());
                    });
                }
            }

            long endTime = System.currentTimeMillis();
            double timeInSeconds = (endTime - startTime) / 1000.0;

            // Отримуємо параметри комп'ютера автоматично
            String sysSpecs = getSystemSpecs();

            SwingUtilities.invokeLater(() -> {
                log("--------------------------------------------------");
                log("💻 Залізо: " + sysSpecs);
                log("⏱ Загальний час виконання: " + String.format("%.2f", timeInSeconds) + " сек.");
                log("--------------------------------------------------");
                log("🎉 Завершено! Всі результати збережено.");
                openFolderBtn.setVisible(true);
                processBtn.setEnabled(true);
            });
        }).start();
    }

    private boolean checkLanguageMatch(String text, String selectedLang) throws InterruptedException, InvocationTargetException {
        int cyrillicCount = 0;
        int latinCount = 0;

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

        if (isUkrSelected && !isTextUkr && latinCount > 0) {
            return showWarningDialog("Ви обрали українську мову, але текст схожий на англійський.\nПродовжити обробку?");
        } else if (!isUkrSelected && isTextUkr && cyrillicCount > 0) {
            return showWarningDialog("Ви обрали англійську мову, але текст схожий на український.\nПродовжити обробку?");
        }

        return true;
    }

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
    private String getSystemSpecs() {
        try {
            // Отримуємо кількість оперативної пам'яті (RAM)
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            long ramGB = Math.round(osBean.getTotalMemorySize() / (1024.0 * 1024.0 * 1024.0));

            // Назва ОС та архітектура
            String os = System.getProperty("os.name");
            int cores = Runtime.getRuntime().availableProcessors();

            // Намагаємось дістати реальну назву процесора
            String cpuName = getCPUName();
            if (cpuName == null || cpuName.isEmpty()) {
                cpuName = System.getProperty("os.arch") + " (" + cores + " потоків)";
            }

            return String.format("%s | CPU: %s | RAM: %d ГБ", os, cpuName, ramGB);
        } catch (Exception e) {
            return "Не вдалося автоматично визначити характеристики системи.";
        }
    }

    private String getCPUName() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Для Windows викликаємо системну утиліту wmic
                Process process = Runtime.getRuntime().exec("wmic cpu get name");
                process.getOutputStream().close();
                java.util.Scanner sc = new java.util.Scanner(process.getInputStream());
                sc.nextLine(); // пропускаємо заголовок "Name"
                return sc.nextLine().trim();
            } else if (os.contains("mac")) {
                // Для Mac (Intel / Apple Silicon) викликаємо sysctl
                Process process = Runtime.getRuntime().exec(new String[]{"sysctl", "-n", "machdep.cpu.brand_string"});
                process.getOutputStream().close();
                java.util.Scanner sc = new java.util.Scanner(process.getInputStream());
                return sc.nextLine().trim();
            }
        } catch (Exception e) {
            // Якщо не вдалося (наприклад, Linux або немає доступу), повертаємо null
        }
        return null;
    }
}