package ru.backendbyjava.generated;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

public class ParsingArtificialLanguage {

    /* =======================
       ====== CONSTANTS ======
       ======================= */

    private static final Set<Operation> ALLOWED_OPERATIONS =
            EnumSet.allOf(Operation.class);

    private static final Map<String, Class<?>> FIELD_TYPES = Map.of(
            "TITLE", String.class,
            "NAME", String.class,
            "WEIGHT", Integer.class,
            "POSTDATE", Date.class
    );

    private static final Map<String, String> FIELD_TO_SQL = Map.of(
            "TITLE", "title_col",
            "NAME", "name_col",
            "WEIGHT", "weight_col",
            "POSTDATE", "post_date"
    );

    private static final Map<Class<?>, Function<String, Object>> VALIDATORS = Map.of(
            String.class, ParsingArtificialLanguage::validateString,
            Integer.class, ParsingArtificialLanguage::validateInteger,
            Date.class, ParsingArtificialLanguage::validateDate
    );

    private static final Map<Class<?>, Function<Object, String>> TRANSLATORS = Map.of(
            String.class, val -> "'" + val + "'",
            Integer.class, val -> val.toString(),
            Date.class, ParsingArtificialLanguage::translateDate
    );

    private static final SimpleDateFormat INPUT_DATE =
            new SimpleDateFormat("yyyy-MM-dd");

    private static final SimpleDateFormat SQL_DATE =
            new SimpleDateFormat("yyyy-MM-dd");

    /* =======================
       ====== MAIN API =======
       ======================= */

    public static String translateExpression(String expression) {
        if (!checkExpression(expression)) {
            throw new IncorrectExpressionException("Invalid expression");
        }

        List<TokenData> tokens = tokenize(normalize(expression));
        StringBuilder sql = new StringBuilder();

        for (TokenData token : tokens) {
            switch (token.type) {
                case FIELD -> sql.append(FIELD_TO_SQL.get(token.value)).append(" ");
                case OPERATION -> sql.append(token.value).append(" ");
                case LOGICAL_OPERATOR -> sql.append(token.value).append(" ");
                case VALUE -> sql.append(token.value).append(" ");
                default -> throw new TranslationToSqlException("Unknown token: " + token.value);
            }
        }

        return sql.toString().trim();
    }

    public static boolean checkExpression(String expression) {
        try {
            List<TokenData> tokens = tokenize(normalize(expression));
            validateTokens(tokens);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /* =======================
       ===== TOKENIZATION ====
       ======================= */

    private static String normalize(String expr) {
        return expr
                .replace("(", " ( ")
                .replace(")", " ) ")
                .trim()
                .toUpperCase();
    }

    private static List<TokenData> tokenize(String normalized) {
        List<TokenData> tokens = new ArrayList<>();
        String[] parts = normalized.split("\\s+");

        for (String part : parts) {
            if (FIELD_TYPES.containsKey(part)) {
                tokens.add(new TokenData(TokenType.FIELD, part));
            } else if (Operation.fromSymbol(part) != null) {
                tokens.add(new TokenData(TokenType.OPERATION, Operation.fromSymbol(part).getSql()));
            } else if (part.equals("AND") || part.equals("OR")) {
                tokens.add(new TokenData(TokenType.LOGICAL_OPERATOR, part));
            } else if (part.equals("(") || part.equals(")")) {
                tokens.add(new TokenData(TokenType.PARENTHESIS, part));
            } else {
                tokens.add(new TokenData(TokenType.VALUE, part));
            }
        }
        return tokens;
    }

    /* =======================
       ===== VALIDATION ======
       ======================= */

    private static void validateTokens(List<TokenData> tokens) {
        Stack<String> stack = new Stack<>();

        for (int i = 0; i < tokens.size(); i++) {
            TokenData token = tokens.get(i);

            if (token.type == TokenType.FIELD) {
                TokenData op = tokens.get(i + 1);
                TokenData value = tokens.get(i + 2);

                Operation operation = Operation.fromSql(op.value);
                if (!ALLOWED_OPERATIONS.contains(operation)) {
                    throw new IncorrectExpressionException("Operation not allowed");
                }

                Class<?> fieldType = FIELD_TYPES.get(token.value);
                Object validatedValue =
                        VALIDATORS.get(fieldType).apply(value.value);

                value.value =
                        TRANSLATORS.get(fieldType).apply(validatedValue);
            }

            if (token.type == TokenType.PARENTHESIS) {
                if (token.value.equals("(")) stack.push("(");
                else if (stack.isEmpty()) throw new IncorrectExpressionException("Unbalanced brackets");
                else stack.pop();
            }
        }

        if (!stack.isEmpty()) {
            throw new IncorrectExpressionException("Unbalanced brackets");
        }
    }

    /* =======================
       ===== VALIDATORS ======
       ======================= */

    private static Object validateString(String value) {
        return value.replace("'", "");
    }

    private static Object validateInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IncorrectExpressionException("Invalid integer: " + value);
        }
    }

    private static Object validateDate(String value) {
        try {
            return INPUT_DATE.parse(value);
        } catch (ParseException ex) {
            throw new IncorrectExpressionException("Invalid date: " + value);
        }
    }

    private static String translateDate(Object date) {
        return "'" + SQL_DATE.format((Date) date) + "'";
    }

    /* =======================
       ===== SUPPORTING ======
       ======================= */

    enum TokenType {
        FIELD,
        OPERATION,
        LOGICAL_OPERATOR,
        VALUE,
        PARENTHESIS
    }

    enum Operation {
        GREATER(">"),
        LESS("<"),
        EQUAL("="),
        LIKE("LIKE");

        private final String sql;

        Operation(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }

        public static Operation fromSymbol(String symbol) {
            return Arrays.stream(values())
                    .filter(o -> o.name().equals(symbol) || o.sql.equals(symbol))
                    .findFirst()
                    .orElse(null);
        }

        public static Operation fromSql(String sql) {
            return Arrays.stream(values())
                    .filter(o -> o.sql.equals(sql))
                    .findFirst()
                    .orElse(null);
        }
    }

    static class TokenData {
        TokenType type;
        String value;

        TokenData(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    /* =======================
       ===== EXCEPTIONS ======
       ======================= */

    static class IncorrectExpressionException extends RuntimeException {
        public IncorrectExpressionException(String message) {
            super(message);
        }
    }

    static class TranslationToSqlException extends RuntimeException {
        public TranslationToSqlException(String message) {
            super(message);
        }
    }
}

