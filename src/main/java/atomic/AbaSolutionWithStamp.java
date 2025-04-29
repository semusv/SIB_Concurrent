package atomic;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Решение проблемы ABA с использованием AtomicStampedReference.
 * Метка (stamp) позволяет отслеживать, было ли изменение состояния,
 * даже если значение вернулось к исходному.
 */
@Slf4j
public class AbaSolutionWithStamp {

    static class Resource {
        final String value;

        Resource(String value) {
            this.value = value;
            log.debug("Создан Resource: {}", value);
        }
    }

    // Храним ссылку на объект + метку (версию)
    private final AtomicStampedReference<Resource> stampedRef =
            new AtomicStampedReference<>(new Resource("A"), 0);

    public void demonstrateAbaDetection() {
        // Шаг 1: Поток 1 запоминает начальное состояние и метку
        int[] initialStamp = new int[1];
        Resource initial = stampedRef.get(initialStamp);
        log.info("Поток 1 - начальное значение: {}, метка: {}",
                initial.value, initialStamp[0]);

        // Шаг 2: Поток 2 имитирует ABA (A -> B -> A)
        new Thread(() -> {
            // Изменяем A -> B и увеличиваем метку
            stampedRef.set(new Resource("B"), initialStamp[0] + 1);
            log.info("Поток 2 - изменил A -> B, новая метка: {}", initialStamp[0] + 1);

            // Возвращаем A, но с новой меткой
            stampedRef.set(new Resource("A"), initialStamp[0] + 2);
            log.info("Поток 2 - вернул A, новая метка: {}", initialStamp[0] + 2);
        }).start();

        // Даем время потоку 2 выполнить изменения
        try { Thread.sleep(50); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Шаг 3: Поток 1 пытается выполнить CAS
        Resource newValue = new Resource("C");
        boolean success = stampedRef.compareAndSet(
                initial,          // Ожидаемый объект
                newValue,          // Новый объект
                initialStamp[0],   // Ожидаемая метка
                initialStamp[0] + 1 // Новая метка
        );

        log.info("Поток 1 - CAS результат: {}. Текущее значение: {}, метка: {}",
                success,
                stampedRef.getReference().value,
                stampedRef.getStamp());
    }

    public static void main(String[] args) {
        new AbaSolutionWithStamp().demonstrateAbaDetection();
    }
}