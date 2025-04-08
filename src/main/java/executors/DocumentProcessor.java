package executors;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * FixedThreadPool
 *
 * Система параллельной обработки документов с использованием ThreadPool.
 *
 * <p>Особенности реализации:
 * <ul>
 *   <li>Использует FixedThreadPool для контроля количества одновременно обрабатываемых документов</li>
 *   <li>Обрабатывает документы параллельно с помощью Stream API</li>
 *   <li>Реализует механизм graceful shutdown с таймаутами</li>
 *   <li>Обрабатывает возможные ошибки выполнения задач</li>
 * </ul>
 */
public class DocumentProcessor {
    // Количество потоков соответствует количеству ядер CPU (оптимально для CPU-bound задач)
    private static final int THREAD_COUNT = 4;

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        // 1. Инициализация пула потоков
        // FixedThreadPool выбран для строгого контроля ресурсов
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // 2. Подготовка данных
        // В реальной системе документы могут поступать из:
        // - Базы данных
        // - Файловой системы
        // - Внешнего API
        List<String> documents = List.of(
                "doc1.pdf", "doc2.pdf", "doc3.pdf",
                "doc4.pdf", "doc5.pdf", "doc6.pdf"
        );

        try {
            // 3. Параллельная обработка документов
            // Используем Stream API для удобного преобразования и обработки
            List<Future<String>> processingResults = documents.stream()
                    // Преобразуем каждый документ в Future с результатом обработки
                    .map(doc -> executor.submit(() -> processDocument(doc)))
                    .collect(Collectors.toList());

            // 4. Получение и обработка результатов
            for (Future<String> result : processingResults) {
                try {
                    // Таймаут 5 секунд на обработку каждого документа
                    // Предотвращает бесконечное ожидание "зависших" задач
                    System.out.println(result.get(5, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    System.err.println("Обработка документа заняла слишком много времени");
                } catch (ExecutionException e) {
                    // Обработка ошибок, возникших при выполнении задачи
                    System.err.println("Ошибка обработки: " + e.getCause().getMessage());
                } catch (InterruptedException e) {
                    // Корректная обработка прерывания потока
                    Thread.currentThread().interrupt();
                    System.err.println("Обработка прервана");
                }
            }
        } finally {
            // 5. Завершение работы пула потоков
            // Обязательно в finally-блоке для гарантированного освобождения ресурсов
            shutdownExecutor(executor);
        }
    }

    /**
     * Обрабатывает один документ.
     *
     * @param documentName имя документа для обработки
     * @return результат обработки
     * @throws Exception в случае ошибок обработки
     */
    private static String processDocument(String documentName) throws Exception {
        // Логирование начала обработки (в реальной системе - SLF4J)
        System.out.println("Начата обработка: " + documentName +
                " в потоке " + Thread.currentThread().getName());

        // Имитация времени обработки (500-2000 мс)
        // В реальной системе здесь может быть:
        // - Парсинг PDF
        // - Оптическое распознавание (OCR)
        // - Извлечение данных
        Thread.sleep(ThreadLocalRandom.current().nextInt(500, 2000));

        // Имитация ошибки для тестирования обработки исключений
        if (documentName.equals("doc3.pdf")) {
            throw new IllegalStateException("Ошибка при обработке " + documentName);
        }

        return "Успешно обработан: " + documentName;
    }

    /**
     * Безопасно завершает работу ExecutorService.
     *
     * @param executor сервис для завершения
     */
    private static void shutdownExecutor(ExecutorService executor) {
        // 1. Запрещаем добавление новых задач
        executor.shutdown();

        try {
            // 2. Даем время на завершение текущих задач
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                // 3. Принудительное завершение при превышении таймаута
                executor.shutdownNow();

                // 4. Дополнительное ожидание для ответа на прерывание
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Пул потоков не завершился корректно");
                }
            }
        } catch (InterruptedException e) {
            // 5. Обработка прерывания во время ожидания
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}