package atomic;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Демонстрация реальной проблемы ABA, где CAS ошибочно проходит.
 * Ключевой момент: повторное использование ОДНОГО И ТОГО ЖЕ объекта.
 */
@Slf4j
public class RealAbaProblemDemo {

    static class Resource {
        String value; // Не final, чтобы можно было изменять
        Resource(String value) {
            this.value = value;
            log.debug("Создан Resource: {}", value);
        }
    }

    private final AtomicReference<Resource> atomicRef =
            new AtomicReference<>(new Resource("A"));

    /**
     * 1. Поток 1 читает объект "A" (ссылка X)
     * 2. Поток 2 меняет X.value = B → A (но это тот же объект!)
     * 3. Поток 1 делает CAS(X, Y) - успешно, хотя значение менялось!
     */
    public void demonstrateRealAba() {
        // Поток 1: Запоминаем ССЫЛКУ на объект "A"
        Resource initial = atomicRef.get();
        log.info("Поток 1 - начальное значение: {}", initial.value);

        // Поток 2: Меняем состояние ТОГО ЖЕ объекта (A → B → A)
        new Thread(() -> {
            initial.value = "B"; // Изменяем существующий объект!
            log.info("Поток 2 - изменил A → B (без создания нового объекта)");

            initial.value = "A"; // Возвращаем исходное значение
            log.info("Поток 2 - вернул B → A (тот же объект)");
        }).start();

        // Даем время потоку 2 выполнить изменения
        try { Thread.sleep(50); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Поток 1: Пытаемся обновить ссылку (но CAS пройдет!)
        boolean success = atomicRef.compareAndSet(initial, new Resource("C"));
        log.info("Поток 1 - CAS результат: {}. Текущее значение: {}",
                success, atomicRef.get().value);
    }

    public static void main(String[] args) {
        new RealAbaProblemDemo().demonstrateRealAba();
    }
}