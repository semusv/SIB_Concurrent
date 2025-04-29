package executors;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Обработчик HTTP-запросов с использованием Java HttpClient и CompletableFuture.
 * Демонстрирует:
 * - Параллельные запросы к REST API
 * - Обработку JSON-ответов
 * - Таймауты и обработку ошибок
 */
@Slf4j
public class HttpRequestProcessor {
    // Тестовые API-сервисы
    private static final List<String> API_ENDPOINTS = List.of(
            "https://api.genderize.io/?name=vadim",            // Тестовый JSON-ресурс
            "https://httpbin.org/get",                            // Сервис для тестирования HTTP
            "https://api.agify.io/?name=michael",                 // Предсказание возраста по имени
            "https://catfact.ninja/fact"                          // Случайные факты о котах
    );

    // Таймауты (в миллисекундах)
    private static final int CONNECT_TIMEOUT = 2000;
    private static final int REQUEST_TIMEOUT = 5000;

    public static void main(String[] args) {
        // 1. Пул потоков для I/O-bound операций
        ExecutorService executor = Executors.newSingleThreadExecutor();
        log.info("Начинаем");
        // 2. Настройка HTTP-клиента
        HttpClient client = HttpClient.newBuilder()
                .executor(executor)
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
                .version(HttpClient.Version.HTTP_2)
                .build();

        // 3. Создание асинхронных запросов
        List<CompletableFuture<String>> futureRequests = API_ENDPOINTS.stream()
                .map(url -> {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofMillis(REQUEST_TIMEOUT))
                            .GET()
                            .build();

                    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApplyAsync(response -> {
                                // Логирование статуса ответа
                                log.info("[{}] {} - Status: {}", Thread.currentThread().getName(), url, response.statusCode());

                                // Возвращаем тело ответа (или сообщение об ошибке)
                                return response.statusCode() == 200
                                        ? processResponse(url, response.body())
                                        : "Error: " + response.statusCode();
                            }, executor)
                            .exceptionally(ex -> {
                                // Обработка ошибок выполнения запроса
                                log.error("Request failed: {} - {}", url, ex.getMessage());
                                return "Failed: " + ex.getMessage();
                            });
                })
                .collect(Collectors.toList());

        // 4. Ожидание всех результатов
        log.info("Ожидаем завершение всего");
        CompletableFuture.allOf(futureRequests.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    log.info("\n=== Результаты запросов ===");
                    futureRequests.forEach(f -> {
                        try {
                            log.info(f.get());
                        } catch (Exception e) {
                            log.error("Error getting result: {}", e.getMessage());
                        }
                    });
                })
                .join(); // Блокировка для демонстрации (в реальном приложении не нужно)

        // 5. Graceful shutdown
        executor.shutdown();
    }

    /**
     * Обработка JSON-ответа (упрощенная реализация)
     */
    private static String processResponse(String url, String body) {
        // В реальном проекте здесь может быть парсинг JSON (Jackson/Gson)
        if (url.contains("jsonplaceholder")) {
            return "Post Data: " + body.substring(0, Math.min(50, body.length())) + "...";
        } else if (url.contains("httpbin")) {
            return "Request Info: " + body.substring(0, 100) + "...";
        } else if (url.contains("agify")) {
            return "Age Prediction: " + body;
        } else {
            return "Response: " + body;
        }
    }


}
