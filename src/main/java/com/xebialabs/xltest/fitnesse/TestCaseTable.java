package com.xebialabs.xltest.fitnesse;

import fitnesse.testsystems.slim.HtmlTable;
import fitnesse.testsystems.slim.HtmlTableScanner;
import fitnesse.wiki.WikiPage;
import org.htmlparser.Node;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import java.util.*;

/**
 * Read a table in the format useable for event generation.
 */
public class TestCaseTable implements Iterable<Map<String, String>>{
    private final HtmlTable table;

    public TestCaseTable(HtmlTable table) {
        this.table = table;
    }

    public static TestCaseTable fromPage(WikiPage wikiPage) {
        String html = wikiPage.readOnlyData().getHtml();
        HtmlTableScanner scanner = new HtmlTableScanner(html);
        Iterator<HtmlTable> iterator = scanner.iterator();
        if (iterator.hasNext()) {
            return new TestCaseTable(iterator.next());
        } else {
            return new TestCaseTable(null);
        }
    }

    public int rows() {
        return table != null ? table.getRowCount() - 1 : 0;
    }

    public boolean isEmpty() {
        return rows() == 0;
    }

    public List<Map<String, String>> asKeyValuePairs() {
        return table != null ? asList(this) : new LinkedList<Map<String, String>>();
    }

    @Override
    public Iterator<Map<String, String>> iterator() {

        TableTag tableTag = table.getTableNode();
        final NodeList rows = getRows(tableTag);
        final int rowCount = rows.size();
        final NodeList headRow = getCells(rows.elementAt(0));
        return new Iterator<Map<String, String>>() {
            int rowNumber = 1;
            @Override
            public boolean hasNext() {
                return rowNumber < rowCount;
            }

            @Override
            public Map<String, String> next() {
                Map<String, String> cells = new TreeMap<String, String>();

                NodeList bodyRow = getCells(rows.elementAt(rowNumber++));
                for (int col = 0; col < headRow.size(); col++) {
                    Node column = headRow.elementAt(col);
                    NodeList children = column.getChildren();
                    if (children == null) {
                        // No label name
                        continue;
                    }
                    String key = children.asString().toLowerCase();
                    if (col < bodyRow.size()) {
                        Node cell = bodyRow.elementAt(col);
                        NodeList cellNodeList = cell.getChildren();
                        if (cellNodeList == null) {
                            // No content, place some default stuff.
                            cells.put(key, "");
                            continue;
                        }
                        String value = cellNodeList.asString();
                        cells.put(key, value);
                        NodeList links = getLinks(cell);
                        if (links.size() > 0) {
                            LinkTag link = (LinkTag) links.elementAt(0);
                            cells.put(key + "_link", link.getLink());
                        }
                    } else {
                        cells.put(key, "");
                    }
                }

                return cells;
            }

            @Override
            public void remove() {
                // Can not remove...
            }
        };
    }

    private static NodeList getRows(TableTag tableNode) {
        return tableNode.getChildren().extractAllNodesThatMatch(new NodeClassFilter(TableRow.class));
    }

    private NodeList getCells(Node rowNode) {
        return rowNode.getChildren().extractAllNodesThatMatch(new NodeClassFilter(TableColumn.class));
    }

    private NodeList getLinks(Node rowNode) {
        return rowNode.getChildren().extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));
    }

    private static <T> List<T> asList(Iterable<T> iterable) {
        List<T> list = new LinkedList<T>();
        Iterator<T> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }
}
