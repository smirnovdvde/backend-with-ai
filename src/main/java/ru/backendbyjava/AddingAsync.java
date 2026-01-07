package ru.backendbyjava;

import java.util.concurrent.CompletableFuture;

public class AddingAsync {
    public CompletableFuture<Integer> addTenToNumberTenTimes(int number) {
        return CompletableFuture.completedFuture(number)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10)
                .thenApply(n -> n + 10);
    }
}
