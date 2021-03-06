package db61b;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static db61b.Utils.*;
import static db61b.Condition.*;

/** A single table in a database.
 *  @author P. N. Hilfinger
 */
class Table implements Iterable<Row> {
    /** A new Table whose columns are given by COLUMNTITLES, which may
     *  not contain dupliace names. */
    Table(String[] columnTitles) {
        for (int i = columnTitles.length - 1; i >= 1; i -= 1) {
            for (int j = i - 1; j >= 0; j -= 1) {
                if (columnTitles[i].equals(columnTitles[j])) {
                    throw error("duplicate column name: %s",
                                columnTitles[i]);
                }
            }
        }
        _columns = columnTitles;
    }

    /** A new Table whose columns are give by COLUMNTITLES. */
    Table(List<String> columnTitles) {
        this(columnTitles.toArray(new String[columnTitles.size()]));
    }

    /** Return the number of columns in this table. */
    public int columns() {
        return _columns.length;
    }

    /** Return the title of the Kth column.  Requires 0 <= K < columns(). */
    public String getTitle(int k) {
        return _columns[k];
    }

    /** Change the name of this table as K. */
    public void changeName(String k) {
        myName = k;
    }

    /** Return the number of the column whose title is TITLE, or -1 if
     *  there isn't one. */
    public int findColumn(String title) {
        int k = -1;
        for (int i = 0; i < _columns.length; i++) {
            if (_columns[i].equals(title)) {
                k = i;
                break;
            }
        }
        return k;
    }

    /** Return the number of Rows in this table. */
    public int size() {
        return _rows.size();
    }

    /** Returns an iterator that returns my rows in an unspecfied order. */
    @Override
    public Iterator<Row> iterator() {
        return _rows.iterator();
    }

    /** Add ROW to THIS if no equal row already exists.  Return true if anything
     *  was added, false otherwise. */
    public boolean add(Row row) {
        if (row.size() != this.columns()) {
            throw new DBException("Row length not equals to column length.");
        }
        if (_rows.contains(row)) {
            return false;
        } else {
            _rows.add(row);
            return true;
        }
    }

    /** Read the contents of the file NAME.db, and return as a Table.
     *  Format errors in the .db file cause a DBException. */
    static Table readTable(String name) {
        BufferedReader input;
        Table table;
        input = null;
        table = null;
        try {
            input = new BufferedReader(new FileReader(name + ".db"));
            String header = input.readLine();
            if (header == null) {
                throw error("missing header in DB file");
            }
            String[] columnNames = header.split(",");
            table = new Table(columnNames);
            while (true) {
                String row = input.readLine();
                if (row != null) {
                    String[] rowNames = row.split(",");
                    Row newRow = new Row(rowNames);
                    table.add(newRow);
                } else {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            throw error("could not find %s.db", name);
        } catch (IOException e) {
            throw error("problem reading from %s.db", name);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    /* Ignore IOException */
                }
            }
        }
        table.myName = name;
        return table;
    }

    /** Write the contents of TABLE into the file NAME.db. Any I/O errors
     *  cause a DBException. */
    void writeTable(String name) {
        PrintStream output;
        output = null;
        try {
            String sep;
            sep = "";
            output = new PrintStream(name + ".db");
            int i;
            for (i = 0; i < columns() - 1; i++) {
                output.print(getTitle(i) + ",");
            }
            output.println(getTitle(i));
            for (Row value: _rows) {
                int j;
                for (j = 0; j < value.size() - 1; j++) {
                    output.print(value.get(j) + ",");
                }
                output.println(value.get(j));
            }
        } catch (IOException e) {
            throw error("trouble writing to %s.db", name);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /** Print my contents on the standard output. */
    void print() {
        System.out.println("Contents of " + myName + ":");
        for (Row value: _rows) {
            for (int j = 0; j < value.size(); j++) {
                if (j == 0) {
                    System.out.print(" ");
                }
                System.out.print(" " + value.get(j));
            }
            System.out.println("");
        }
    }
    /** Print table as search results. */
    void searchPrint() {
        System.out.println("Search results:");
        for (Row value: _rows) {
            for (int j = 0; j < value.size(); j++) {
                if (j == 0) {
                    System.out.print(" ");
                }
                System.out.print(" " + value.get(j));
            }
            System.out.println("");
        }
    }

    /** Return a new Table whose columns are COLUMNNAMES, selected
     *  from this TABLE. */
    Table select(List<String> columnNames) {
        Table result = new Table(columnNames);
        Table[] tables = {this};
        ArrayList<Column> columns = convertCol(columnNames, tables);
        for (Row row1: this) {
            Row[] rows = {row1};
            result.add(new Row(columns, rows));
        }
        return result;
    }

    /** Return a new Table whose columns are COLUMNNAMES, selected
     *  from pairs of rows from this table and from TABLE2. */
    Table select(Table table2, List<String> columnNames) {
        Table result = new Table(columnNames);
        Table[] tables = {this, table2};
        ArrayList<String> commonCols = commonCol(this, table2);
        ArrayList<Column> columns = convertCol(columnNames, tables);
        ArrayList<Column> common1 = convertCol(commonCols, this);
        ArrayList<Column> common2 = convertCol(commonCols, table2);
        for (Row row1: this) {
            for (Row row2: table2) {
                if (equijoin(common1, common2, row1, row2)) {
                    Row[] rows = {row1, row2};
                    result.add(new Row(columns, rows));
                }
            }
        }
        return result;
    }

    /** Return a new Table whose columns are COLUMNNAMES, selected from
     *  rows of this table that satisfy CONDITIONS. */
    Table select(List<String> columnNames, List<Condition> conditions) {
        Table result = new Table(columnNames);
        Table[] tables = {this};
        ArrayList<Column> columns = convertCol(columnNames, tables);
        for (Row row1: this) {
            Row[] rows = {row1};
            if (db61b.Condition.test(conditions, rows)) {
                result.add(new Row(columns, rows));
            }
        }
        return result;
    }

    /** Return a new Table whose columns are COLUMNNAMES, selected
     *  from pairs of rows from this table and from TABLE2 that match
     *  on all columns with identical names and satisfy CONDITIONS. */
    Table select(Table table2, List<String> columnNames,
                 List<Condition> conditions) {
        Table result = new Table(columnNames);
        Table[] tables = {this, table2};
        ArrayList<String> commonCols = commonCol(this, table2);
        ArrayList<Column> columns = convertCol(columnNames, tables);
        ArrayList<Column> common1 = convertCol(commonCols, this);
        ArrayList<Column> common2 = convertCol(commonCols, table2);
        for (Row row1: this) {
            for (Row row2: table2) {
                if (equijoin(common1, common2, row1, row2)) {
                    Row[] rows = {row1, row2};
                    if (db61b.Condition.test(conditions, rows)) {
                        result.add(new Row(columns, rows));
                    }
                }
            }
        }
        return result;
    }

    /** Find the common columns of two Tables: TABLE1 and TABLE2,
     *  return the common titles as an ArrayList. */
    private static ArrayList<String> commonCol(Table table1, Table table2) {
        ArrayList<String> columns = new ArrayList<>();
        for (int i = 0; i < table1.columns(); i++) {
            for (int j = 0; j < table2.columns(); j++) {
                String col1 = table1.getTitle(i);
                String col2 = table2.getTitle(j);
                if (col1.equals(col2)) {
                    columns.add(col1);
                }
            }
        }
        return columns;
    }

    /** Convert COLUMNNAMES to COLUMN of TABLES,
    * return columns as a ArrayList. */
    private static ArrayList<Column> convertCol(List<String> columnNames,
                                                Table... tables) {
        ArrayList<Column> columns = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            columns.add(new Column(columnNames.get(i), tables));
        }
        return columns;
    }

    /** Return true if the columns COMMON1 from ROW1 and COMMON2 from
     *  ROW2 all have identical values.  Assumes that COMMON1 and
     *  COMMON2 have the same number of elements and the same names,
     *  that the columns in COMMON1 apply to this table, those in
     *  COMMON2 to another, and that ROW1 and ROW2 come, respectively,
     *  from those tables. */
    private static boolean equijoin(List<Column> common1, List<Column> common2,
                                    Row row1, Row row2) {
        for (int i = 0; i < common1.size(); i++) {
            String entry1 = common1.get(i).getFrom(row1);
            String entry2 = common2.get(i).getFrom(row2);
            if (!entry1.equals(entry2)) {
                return false;
            }
        }
        return true;
    }

    /** My rows. */
    private HashSet<Row> _rows = new HashSet<>();
    /** my column titles. */
    private String[] _columns = new String[] {};
    /** my name. */
    private String myName;
}

