package ru.backendbyjava.generated;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.backendbyjava.ParsingArtificialLanguage;

public class ParsingArtificialLanguageTest {
    @Test
    void testPositiveDifferentCases() {
        ru.backendbyjava.generated.ParsingArtificialLanguage parsingArtificialLanguage = new ru.backendbyjava.generated.ParsingArtificialLanguage();

        parsingArtificialLanguage.checkExpression("TITLE LIKE '123%'");
        parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' OR WEIGHT > 5");
        parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' OR WEIGHT > 5 AND weight  < 9");
        parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT > 5 AND weight  < 9)");
        parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT > 5 AND weight  < 9 OR (WEIGHT > 10))");
        parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT >= 5 AND weight  <= 9 OR (WEIGHT > 10))");
        parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT > 5 AND weight  < 9 OR (WEIGHT = 10)) AND (POSTDATE = '01.12.2025 12:00'  )");
        parsingArtificialLanguage.checkExpression("TITLE LIKE '12 3%' AND (WEIGHT > 5 AND weight  < 9 OR (WEIGHT = 10)) AND (POSTDATE = '01.12.2025 12:00'  )");
        parsingArtificialLanguage.translateExpression("TITLE LIKE '12 3%' AND (WEIGHT > 5 AND weight  < 9 OR (WEIGHT = 10)) AND (POSTDATE = '01.12.2025 12:00'  )");
        parsingArtificialLanguage.translateExpression("title like '123%'");
    }

    @Test
    void testNegativeDifferentCases() {
        ru.backendbyjava.ParsingArtificialLanguage parsingArtificialLanguage = new ru.backendbyjava.ParsingArtificialLanguage();

        Assertions.assertThrows(ru.backendbyjava.ParsingArtificialLanguage.IncorrectExpressionException.class,
                () -> parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT > 5 AND weight  < 9))"));

        Assertions.assertThrows(ru.backendbyjava.ParsingArtificialLanguage.IncorrectExpressionException.class,
                () -> parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT > 5 AND weight  < 9 OR (WEIGHT > 10)) AND ()"));

        Assertions.assertThrows(ru.backendbyjava.ParsingArtificialLanguage.IncorrectExpressionException.class,
                () -> parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT >= 5 AND weight  <= 9 OR (WEIGHT > ))"));

        Assertions.assertThrows(ru.backendbyjava.ParsingArtificialLanguage.IncorrectExpressionException.class,
                () -> parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT > 5 AND weight  < 9 OR (WEIGHT2 > 10))"));

        Assertions.assertThrows(ru.backendbyjava.ParsingArtificialLanguage.IncorrectExpressionException.class,
                () -> parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (WEIGHT > 5 AND weight  < 9 OR (WEIGHT LIKE 10))"));

        Assertions.assertThrows(ParsingArtificialLanguage.IncorrectExpressionException.class,
                () -> parsingArtificialLanguage.checkExpression("TITLE LIKE '123%' AND (POSTDATE = '01.12.2025 12:00' AND)"));
    }
}
