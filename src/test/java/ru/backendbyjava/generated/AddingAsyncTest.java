package ru.backendbyjava.generated;

import org.junit.jupiter.api.Test;
import ru.backendbyjava.AddingAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddingAsyncTest {
    @Test
    void testAddTenToNumberTenTimesWithCountDownLatch() throws InterruptedException {
        AddingAsync addingAsync = new AddingAsync();
        CountDownLatch latch = new CountDownLatch(1);
        Integer[] result = new Integer[1];

        CompletableFuture<Integer> future = addingAsync.addTenToNumberTenTimes(5);
        future.thenAccept(value -> {
            result[0] = value;
            latch.countDown();
        });

        latch.await();
        assertEquals(105, result[0].intValue());
    }

    @Test
    void testAddTenToNumberTenTimesMultipleOperations() throws InterruptedException {
        AddingAsync addingAsync = new AddingAsync();
        CountDownLatch latch = new CountDownLatch(3);
        Integer[] results = new Integer[3];

        // Test three different operations
        CompletableFuture<Integer> future1 = addingAsync.addTenToNumberTenTimes(0);
        future1.thenAccept(value -> {
            results[0] = value;
            latch.countDown();
        });

        CompletableFuture<Integer> future2 = addingAsync.addTenToNumberTenTimes(10);
        future2.thenAccept(value -> {
            results[1] = value;
            latch.countDown();
        });

        CompletableFuture<Integer> future3 = addingAsync.addTenToNumberTenTimes(-5);
        future3.thenAccept(value -> {
            results[2] = value;
            latch.countDown();
        });

        latch.await();
        assertEquals(100, results[0].intValue());
        assertEquals(110, results[1].intValue());
        assertEquals(95, results[2].intValue());
    }
}
