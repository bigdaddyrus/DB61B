package db61b;

import static org.junit.Assert.*;
import org.junit.Test;

/** Basic Tests for Table */
public class ConditionTest {

    @Test
    public void testTest() {
    /* Run the Add Row tests in this file. */
        Table t = new Table(new String[] {"One", "Two", "Three"});
        Row r = new Row(new String[] {"101", "Test", "Yay"});
        Row k = new Row(new String[] {"102", "Testy", "Yayy"});
        t.add(r);
        t.add(k);
        Table[] tables = {t};
        Column col1 = new Column("One", tables);
        String rel = "=";
        String val2 = "101";
        Condition cond = new Condition(col1, rel, val2);
        assertTrue(cond.test(r));
        assertFalse(cond.test(k));
        rel = ">";
        cond = new Condition(col1, rel, val2);
        assertFalse(cond.test(r));
        assertTrue(cond.test(k));
    }
    public static void main(String... args) {
        System.exit(ucb.junit.textui.runClasses(ConditionTest.class));
    }
}
