# Backend with AI

This project demonstrates asynchronous operations and artificial language parsing capabilities and what AI assistants could generate based on it.

## Features

- **Asynchronous Operations**: Implements `CompletableFuture` for non-blocking computations
- **Expression Parsing**: Parses and validates artificial language expressions

## Modules

### AddingAsync
- Asynchronously adds 10 to a number 10 times
- Uses `CompletableFuture` for non-blocking execution

### ParsingArtificialLanguage
- Parses and validates expressions with LIKE, AND, OR, and comparison operators
- Supports date and numeric comparisons
- Throws exceptions for malformed expressions

## Usage

### AddingAsync
```java
AddingAsync addingAsync = new AddingAsync();
CompletableFuture<Integer> result = addingAsync.addTenToNumberTenTimes(5);