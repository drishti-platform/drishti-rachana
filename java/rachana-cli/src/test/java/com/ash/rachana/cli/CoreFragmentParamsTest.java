/*
 * Copyright (c) 2026 Ashutosh Sinha <ajsinha@gmail.com>. All rights reserved.
 * Drishti Rachana. Proprietary: see the LICENSE file at the root of this repository.
 */
package com.ash.rachana.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ash.rachana.Rachana;
import com.ash.rachana.SutraExpressions;
import com.ash.rachana.lib.LibraryResolver;
import com.ash.rachana.lib.LibrarySet;
import com.ash.rachana.model.Sutra;
import com.ash.rachana.table.LiveScreen;
import com.ash.rachana.view.ViewJson;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The panel keys of the core fragments (library core 1.1.0): data-table and record-card take key, code, empty, when, span (and where) as
 * optional parameters, and a key the use leaves out is left out of the panel; record-card's head is optional; tabbed-views fills a slot
 * inside its tabs panel. These uses are not examples of the library (an example per fragment is the library's shape), so they are proved here.
 */
class CoreFragmentParamsTest {

    private static String view(String sutra) throws Exception {
        Path repo = Paths.get(System.getProperty("rachana.repo.root")).toAbsolutePath().normalize();
        Rachana rachana = Rachana.core().language("1.2").libraries(LibrarySet.of(LibraryResolver.folder(repo.resolve("libraries"))));
        Sutra parsed = rachana.parser().parse(sutra, "t.sutra.yaml", "library");
        assertEquals(List.of(), new SutraExpressions(rachana.compiler()).check(parsed));
        assertEquals(List.of(), parsed.advisories());
        return ViewJson.write(new LiveScreen(rachana, parsed, LocalDate.of(2026, 1, 15), null, Map.of()).view());
    }

    private static final String HEAD = """
            rachana: 1.2
            sutra: params-test
            version: 1
            libraries: [core@1]
            title: { id: "'T'" }
            data:
              orders: { rows: [ { ORDER_ID: A1, AMT_USD: 120 }, { ORDER_ID: A2, AMT_USD: 0 }, { ORDER_ID: A3, AMT_USD: 80 } ] }
              company: { rows: [ { FOUNDED: 1999, STAFF: 4200 } ] }
              firm: { rows: [ { name: Acme, subtitle: Maker, status: ok, FOUNDED: 1999 } ] }
              legs: { rows: [ { LEG: 1, LABEL: Fixed }, { LEG: 2, LABEL: Floating } ] }
            panels:
            """;

    @Test
    void dataTableTakesTheKeysAndLeavesOutTheOnesNotGiven() throws Exception {
        String full = view(HEAD + """
                  - use: core/data-table@1
                    with:
                      rows: $.orders
                      columns: [ { label: Order, bind: "@.ORDER_ID" } ]
                      key: F2
                      code: ORD
                      empty: No open orders
                      span: 6
                      where: "lambda r: r.AMT_USD > 0"
                """);
        assertTrue(full.contains("\"key\":\"F2\"") && full.contains("\"code\":\"ORD\"") && full.contains("\"span\":6"), full);
        assertTrue(full.contains("A1") && full.contains("A3") && !full.contains("\"text\":\"A2\""), full);
        String bare = view(HEAD + """
                  - use: core/data-table@1
                    with: { rows: $.orders, columns: [ { label: Order, bind: "@.ORDER_ID" } ] }
                """);
        assertFalse(bare.contains("\"key\":") || bare.contains("\"code\":") || bare.contains("\"span\":"), bare);
        assertTrue(bare.contains("A2"), bare);
    }

    @Test
    void aFalseWhenLeavesTheTableOut() throws Exception {
        String v = view(HEAD + """
                  - use: core/data-table@1
                    with: { rows: $.orders, columns: [ { label: Order, bind: "@.ORDER_ID" } ], when: "1 > 2" }
                """);
        assertFalse(v.contains("A1"), v);
    }

    @Test
    void recordCardWithoutAHeadIsAPlainRecordView() throws Exception {
        String v = view(HEAD + """
                  - use: core/record-card@1
                    with:
                      record: "$.company[0]"
                      title: Company facts
                      facts: [ { label: Founded, bind: "@.FOUNDED" }, { label: Employees, bind: "@.STAFF", fmt: amount0 } ]
                      key: F3
                      code: CO
                      span: 4
                """);
        assertTrue(v.contains("Company facts") && v.contains("\"key\":\"F3\"") && v.contains("\"code\":\"CO\"") && v.contains("\"span\":4"), v);
        assertTrue(v.contains("1999") && v.contains("4,200"), v);
        assertFalse(v.contains("\"id\":\"state\""), v);                       // no status: no status panel
    }

    @Test
    void tabbedViewsHoldsTheSlotInsideItsTabs() throws Exception {
        String v = view(HEAD + """
                  - use: core/tabbed-views@1
                    with:
                      each: $.legs
                      tab-title: "lambda p: 'Leg ' + p.LEG + ' - ' + p.LABEL"
                      layout: columns
                      key: F4
                    slots:
                      body:
                        - { id: terms, kind: kv, columns: [ { label: Type, bind: "@.LABEL" } ] }
                """);
        assertTrue(v.contains("Leg 1 - Fixed") && v.contains("Leg 2 - Floating") && v.contains("Floating") && v.contains("\"key\":\"F4\""), v);
    }

    @Test
    void aUseKeepsItsOwnPanelIdWhenItGivesOne() throws Exception {
        String table = view(HEAD + """
                  - use: core/data-table@1
                    as: holidays
                    with: { id: holidays, rows: $.orders, columns: [ { label: Order, bind: "@.ORDER_ID" } ], key: F5 }
                """);
        assertTrue(table.contains("\"id\":\"holidays\"") && !table.contains("holidays-table"), table);
        String card = view(HEAD + """
                  - use: core/record-card@1
                    as: company
                    with:
                      id: company
                      record: "$.firm[0]"
                      facts: [ { label: Founded, bind: "@.FOUNDED" } ]
                """);
        assertTrue(card.contains("\"id\":\"company\"") && card.contains("company-state") && !card.contains("company-card"), card);
        String tabs = view(HEAD + """
                  - use: core/tabbed-views@1
                    as: legs
                    with: { id: legs, each: $.legs, tab-title: "lambda p: 'Leg ' + p.LEG" }
                    slots:
                      body:
                        - { id: terms, kind: kv, columns: [ { label: Type, bind: "@.LABEL" } ] }
                """);
        assertTrue(tabs.contains("\"id\":\"legs\"") && !tabs.contains("legs-views"), tabs);
        String scoped = view(HEAD + """
                  - use: core/data-table@1
                    as: plain
                    with: { rows: $.orders, columns: [ { label: Order, bind: "@.ORDER_ID" } ] }
                """);
        assertTrue(scoped.contains("\"id\":\"plain-table\""), scoped);
    }
}
