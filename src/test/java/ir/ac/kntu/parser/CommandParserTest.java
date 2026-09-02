package ir.ac.kntu.parser;

import ir.ac.kntu.exception.SyntaxErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
    }

    @Test
    void testParseCreateCommand() {
        String input = "CREATE students[id int, name str, grade dbl, approved int]";
        ParsedCommand cmd = parser.parse(input);

        assertEquals(CommandType.CREATE, cmd.getType());
        assertEquals("students", cmd.getTableName());
        assertEquals(4, cmd.getArguments().size());
        assertEquals("id int", cmd.getArguments().get(0));
        assertEquals("approved int", cmd.getArguments().get(3));
    }

    @Test
    void testParseAddWithArbitrarySpacesAndQuotes() {
        String input = "  add   students { id 101 , name \"Ali Reza\", grade 18.5 }  ";
        ParsedCommand cmd = parser.parse(input);

        assertEquals(CommandType.ADD, cmd.getType());
        assertEquals("students", cmd.getTableName());
        assertEquals("101", cmd.getVariables().get("id"));
        assertEquals("\"Ali Reza\"", cmd.getVariables().get("name"));
        assertEquals("18.5", cmd.getVariables().get("grade"));
    }

    @Test
    void testParseGetWithCondition() {
        String input = "get students (grade > 15 + bonus) [id, name]";
        ParsedCommand cmd = parser.parse(input);

        assertEquals(CommandType.GET, cmd.getType());
        assertEquals("students", cmd.getTableName());
        assertEquals(1, cmd.getParameters().size());
        assertEquals("grade > 15 + bonus", cmd.getParameters().get(0));
        assertEquals(2, cmd.getArguments().size());
        assertEquals("id", cmd.getArguments().get(0));
    }

    @Test
    void testParseDropCommand() {
        String input = "drop students";
        ParsedCommand cmd = parser.parse(input);

        assertEquals(CommandType.DROP, cmd.getType());
        assertEquals("students", cmd.getTableName());
        assertTrue(cmd.getArguments().isEmpty());
    }

    @Test
    void testParseQuitCommand() {
        ParsedCommand cmd = parser.parse("  QUIT  ");
        assertEquals(CommandType.QUIT, cmd.getType());
        assertNull(cmd.getTableName());
    }

    @Test
    void testInvalidCommandThrowsException() {
        assertThrows(SyntaxErrorException.class, () -> parser.parse("FETCH students[id]"));
    }

    @Test
    void testMissingTableNameThrowsException() {
        assertThrows(SyntaxErrorException.class, () -> parser.parse("create [id int]"));
    }
}