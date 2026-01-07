package ru.backendbyjava;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.backendbyjava.ParsingArtificialLanguage.Operation.*;

public class ParsingArtificialLanguage {
    private static final Set<Operation> ALLOWED_OPERATIONS = Set.of(GREATER, LESS, EQUAL, GREATER_OR_EQUAL,
            LESS_OR_EQUAL, LIKE);
    private static final Map<Class<?>, Set<Operation>> ALLOWED_OPERATION_MAP = Map.of(
            Integer.class, Set.of(GREATER, LESS, EQUAL, GREATER_OR_EQUAL, LESS_OR_EQUAL),
            Date.class, Set.of(GREATER, LESS, EQUAL, GREATER_OR_EQUAL, LESS_OR_EQUAL),
            String.class, Set.of(GREATER, LESS, EQUAL, GREATER_OR_EQUAL, LESS_OR_EQUAL, LIKE)
    );
    private static final Set<String> FIELD_NAMES = Set.of("TITLE", "NAME", "WEIGHT", "POSTDATE");
    private static final Map<String, Class<?>> FIELD_TYPE_MAP = Map.of(
            "TITLE", String.class,
            "NAME", String.class,
            "WEIGHT", Integer.class,
            "POSTDATE", Date.class
    );

    private static final Map<Class<?>, Consumer<String>> FIELD_TYPE_VALIDATORS_MAP = Map.of(
            Integer.class, ParsingArtificialLanguage::checkInteger,
            String.class, ParsingArtificialLanguage::checkString,
            Date.class, ParsingArtificialLanguage::checkDate
    );

    private static final Map<Class<?>, Function<String, String>> FIELD_TYPE_TRANSLATORS_MAP = Map.of(
            Date.class, ParsingArtificialLanguage::translateDateToSqlFormat
    );

    private static final Map<String, String> PSEUDO_FIELD_SQL_MAP = Map.of(
            "TITLE", "ARTICLE.TITLE",
            "NAME", "ARTICLE.NAME",
            "WEIGHT", "ARTICLE.WEIGHT",
            "POSTDATE", "ARTICLE_POST_DATE"
    );

    private static final Set<String> LOGICAL_OPERATIONS = Set.of("OR", "AND");
    private static final Set<Character> TECHNICAL_TOKENS = Set.of('>', '<', '=', '(', ')');
    private static final Set<String> TYPES_OF_CONDENSED_EXPRESSION = Set.of("FIELD OPERATION VALUE",
            "CONDENSED_EXPRESSION LOGOPER FIELD OPERATION VALUE", "CONDENSED_EXPRESSION LOGOPER CONDENSED_EXPRESSION");

    private static final Set<String> TYPES_OF_EXPRESSION = Set.of("FIELD", "FIELD OPERATION", "CONDENSED_EXPRESSION LOGOPER",
            "CONDENSED_EXPRESSION", "CONDENSED_EXPRESSION LOGOPER FIELD", "CONDENSED_EXPRESSION LOGOPER FIELD OPERATION",
            "CONDENSED_EXPRESSION LOGOPER FIELD OPERATOR");
    private static final String CONDENSED_EXPRESSION = "CONDENSED_EXPRESSION";
    private static final String OPERATION = "OPERATION";
    private static final String FIELD = "FIELD";
    private static final String LOGICAL_OPERATION = "LOGOPER";
    private static final String VALUE = "VALUE";
    private static final String EMPTY_STRING = "";
    private static final String SPACE = " ";
    private static final String TWO_SPACES = "  ";
    private static final String OPEN_BRACKET = "(";
    private static final String CLOSE_BRACKET = ")";
    private static final String DATE_PATTERN = "dd.MM.yyyy HH:mm";
    private static final String SQL_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String VALUE_WITH_QUOTE_TOKEN_SEPARATOR = "' ";

    private String currentField = EMPTY_STRING;
    private Operation currentOperation = EMPTY_OPERATION;
    private String currentValue = EMPTY_STRING;
    private List<TokenData> tokenDataList = new ArrayList<>();

    public String translateExpression(String expression) {
        String resultExpression = expression;
        if (checkExpression(expression)) {
            for (Map.Entry<String, String> entry : PSEUDO_FIELD_SQL_MAP.entrySet()) {
                resultExpression = resultExpression.replace(entry.getKey(), entry.getValue());
            }
        }
        resultExpression = translateFieldValues(resultExpression);
        return resultExpression;
    }

    private String translateFieldValues(String expression) {
        for (Map.Entry<Class<?>, Function<String, String>> entry : FIELD_TYPE_TRANSLATORS_MAP.entrySet()) {
            for (int i = 0; i < tokenDataList.size(); i++) {
                TokenData item = tokenDataList.get(i);
                if (item.tokenType.equals(TokenType.FIELD)) {
                    Class<?> fieldDataType = FIELD_TYPE_MAP.get(item.token);
                    if (entry.getKey().equals(fieldDataType)) {
                        TokenData tokenDataValue = tokenDataList.get(i + 2);
                        String translatedValue = entry.getValue().apply(tokenDataValue.token);
                        expression = expression.replace(tokenDataValue.token, translatedValue);
                    }
                }
            }
        }
        return expression;
    }

    private static String translateDateToSqlFormat(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter formatterForSql = DateTimeFormatter.ofPattern(SQL_DATE_PATTERN);
        try {
            value = value.replace("'", "");
            LocalDateTime localDateTime = LocalDateTime.parse(value, formatter);
            return "'" + formatterForSql.format(localDateTime) + "'";
        } catch (DateTimeParseException e) {
            throw new TranslationToSqlException("Error during translation date");
        }
    }

    public boolean checkExpression(String expression) {
        if (expression == null) {
            return true;
        }
        if (expression.isEmpty()) {
            return true;
        }
        String interimExpression = normalizeExpression(expression);
        String condensedExpression = processNormalizedExpression(interimExpression);

        condensedExpression = condenseExpression(condensedExpression).trim();
        if (!CONDENSED_EXPRESSION.equals(condensedExpression)) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
        return true;
    }

    private String processNormalizedExpression(String normalizedExpression) {
        Deque<String> stack = new ArrayDeque<>();
        String condensedExpression = EMPTY_STRING;
        int tokenCounter = 0;
        List<TokenData> tokenDataList = new ArrayList<>();

        while (!normalizedExpression.isEmpty()) {
            StringBuilder currentBuilder = new StringBuilder(condensedExpression);
            currentBuilder.append(SPACE);

            String separator = determineSeparator(tokenCounter, tokenDataList);

            int nextSpace = normalizedExpression.indexOf(separator);
            if (nextSpace == -1) {
                nextSpace = normalizedExpression.length();
            }
            int indexTo = Math.min(normalizedExpression.length(), nextSpace + separator.length() - 1);
            String token = normalizedExpression.substring(0, indexTo);
            normalizedExpression = normalizedExpression.substring(indexTo).trim();

            TokenType tokenType = encodeToken(currentBuilder, token);
            if (tokenType != TokenType.UNKNOWN) {
                tokenCounter++;
                tokenDataList.add(new TokenData(tokenType, token));
            }
            if (tokenType.equals(TokenType.FIELD)) {
                token = token.toUpperCase();
            }
            setInternalFields(tokenType, token);

            if (token.equals(OPEN_BRACKET)) {
                stack.push(token);
                stack.push(condensedExpression);
                condensedExpression = EMPTY_STRING;
                currentBuilder = new StringBuilder();
            }
            if (token.equals(CLOSE_BRACKET)) {
                processCloseBracket(stack, currentBuilder);
            }

            condensedExpression = condenseExpression(currentBuilder.toString());

            if (TokenType.VALUE.equals(tokenType)) {
                checkFieldOperationValueConsistency();
            }
        }
        this.tokenDataList = tokenDataList;
        return condensedExpression;
    }

    private String determineSeparator(int tokenCounter, List<TokenData> tokenDataList) {
        String separator = SPACE;
        if (tokenCounter >= 2) {
            TokenData tokenData = tokenDataList.get(tokenCounter - 2);
            if (tokenData.tokenType == TokenType.FIELD &&
                    (FIELD_TYPE_MAP.get(tokenData.token) == Date.class ||
                            FIELD_TYPE_MAP.get(tokenData.token) == String.class
                    )) {
                separator = VALUE_WITH_QUOTE_TOKEN_SEPARATOR;
            }
        }

        return separator;
    }

    private void checkFieldOperationValueConsistency() {
        Class<?> fieldType = FIELD_TYPE_MAP.get(currentField);
        if (fieldType == null) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
        Set<Operation> allowedOperationForField = ALLOWED_OPERATION_MAP.get(fieldType);
        if (allowedOperationForField == null || allowedOperationForField.isEmpty()) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
        if (!allowedOperationForField.contains(currentOperation)) {
            throw new IncorrectExpressionException("Incorrect expression");
        }

        Consumer<String> valueValidator = FIELD_TYPE_VALIDATORS_MAP.get(fieldType);
        if (valueValidator == null) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
        valueValidator.accept(currentValue);
    }

    private static void checkInteger(String value) {
        try {
            Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
    }

    private static void checkString(String value) {
        if (value == null || !value.startsWith("'") || !value.endsWith("'")) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
    }

    private static void checkDate(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        if (value == null || !value.startsWith("'") || !value.endsWith("'")) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
        try {
            value = value.replace("'", "");
            LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
    }

    private void setInternalFields(TokenType tokenType, String token) {
        switch (tokenType) {
            case FIELD:
                currentField = token;
                break;
            case OPERATION:
                currentOperation = findBySymbolicValue(token);
                break;
            case VALUE:
                currentValue = token;
                break;
        }
    }

    private void processCloseBracket(Deque<String> stack, StringBuilder currentBuilder) {
        Deque<String> directTokensQueue = new ArrayDeque<>();
        if (stack.isEmpty()) {
            throw new IncorrectExpressionException("Incorrect expression");
        }
        while (!stack.isEmpty() && !stack.peek().equals(OPEN_BRACKET)) {
            String tokenFromStack = stack.pop();
            if (!tokenFromStack.equals(OPEN_BRACKET)) {
                directTokensQueue.add(tokenFromStack);
            }
        }
        if (!stack.isEmpty() && stack.peek().equals(OPEN_BRACKET)) {
            stack.pop();
        }
        String condensedExpression = EMPTY_STRING;
        while (!directTokensQueue.isEmpty()) {
            condensedExpression = condensedExpression + SPACE +
                    directTokensQueue.pollLast();

            condensedExpression = condenseExpression(condensedExpression);
        }
        currentBuilder.insert(0, SPACE);
        currentBuilder.insert(0, condensedExpression);
    }

    private TokenType encodeToken(StringBuilder currentBuilder, String token) {
        if (ALLOWED_OPERATIONS.contains(findBySymbolicValue(token))) {
            currentBuilder.append(OPERATION);
            return TokenType.OPERATION;
        } else if (FIELD_NAMES.contains(token)) {
            currentBuilder.append(FIELD);
            return TokenType.FIELD;
        } else if (LOGICAL_OPERATIONS.contains(token)) {
            currentBuilder.append(LOGICAL_OPERATION);
            return TokenType.LOGICAL_OPERATOR;
        } else if (!token.equals(OPEN_BRACKET) && !token.equals(CLOSE_BRACKET)) {
            currentBuilder.append(VALUE);
            return TokenType.VALUE;
        }
        return TokenType.UNKNOWN;
    }

    private String normalizeExpression(String expression) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char currChar = expression.charAt(i);
            if (TECHNICAL_TOKENS.contains(currChar) && i + 1 < expression.length() && expression.charAt(i + 1) != '=') {
                sb.append(currChar);
                sb.append(SPACE);
            } else {
                sb.append(currChar);
            }
        }
        return sb.toString()
                .replace("(", " ( ")
                .replace(")", " ) ")
                .replace(">=", " >= ")
                .replace("<=", " <= ")
                .replace(TWO_SPACES, SPACE)
                .trim().toUpperCase();
    }

    private String condenseExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return EMPTY_STRING;
        }
        String interimExpression = expression.replace(TWO_SPACES, SPACE).trim();

        if (TYPES_OF_EXPRESSION.contains(interimExpression)) {
            return expression;
        }

        if (TYPES_OF_CONDENSED_EXPRESSION.contains(interimExpression)) {
            return CONDENSED_EXPRESSION;
        }

        throw new IncorrectExpressionException("Incorrect Expression");
    }

    private static class TokenData {
        private final TokenType tokenType;
        private final String token;

        private TokenData(TokenType tokenType, String token) {
            this.tokenType = tokenType;
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public TokenType getTokenType() {
            return tokenType;
        }
    }

    enum TokenType {
        OPERATION,
        FIELD,
        LOGICAL_OPERATOR,
        VALUE,
        UNKNOWN;
    }

    enum Operation {
        GREATER(">"),
        LESS("<"),
        EQUAL("="),
        GREATER_OR_EQUAL(">="),
        LESS_OR_EQUAL("<="),
        LIKE("LIKE"),
        EMPTY_OPERATION("");

        private final String symbolicValue;

        Operation(String symbolicValue) {
            this.symbolicValue = symbolicValue;
        }

        public String getSymbolicValue() {
            return symbolicValue;
        }

        public static Operation findBySymbolicValue(String symbolicValue) {
            Optional<Operation> optOperation = Arrays.stream(Operation.values()).filter(operation -> operation.getSymbolicValue().equals(symbolicValue))
                    .findFirst();
            return optOperation.orElse(EMPTY_OPERATION);
        }
    }

    public static class IncorrectExpressionException extends RuntimeException {
        public IncorrectExpressionException(String message) {
            super(message);
        }
    }

    public static class TranslationToSqlException extends RuntimeException {
        public TranslationToSqlException(String message) {
            super(message);
        }
    }
}
