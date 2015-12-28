package com.ssl.cltracker.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerforceBranchTree<K> extends BranchTree<K> {

    private static final String STRIP_REG_EXP = "^/*|\\.*$";
    private static final String DELIMITER = "/";
    private static final String PRE_APPEND = "//";
    private static final String POST_APPEND = "";

    public PerforceBranchTree(String node) {
        super(node);
    }

    public PerforceBranchTree(String node, Map<String, BranchTree<K>> child) {
        super(node, child);
    }

    @Override
    public void putChild(String tag) {
        if (child == null) {
            child = new HashMap<String, BranchTree<K>>();
        }

        child.put(tag, new PerforceBranchTree<K>(tag));
    }

    protected String stripOffStrings(String str) {
        return str.replaceAll(STRIP_REG_EXP, "");
    }

    @Override
    protected String getDelimiter() {
        return DELIMITER;
    }

    @Override
    protected String getPreAppend() {
        return PRE_APPEND;
    }

    @Override
    protected String getPostAppend() {
        return POST_APPEND;
    }

    /**
     * TEST!
     */
    public static void main(String args[]) throws Exception {
        String a = "//PROD_NILE/TAB_S2_NA/FLUMEN/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/LauncherFacade_Legacy.java";
        String b = "//PROD_NILE/TAB_S2_NA/FLUMEN/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/Utils.java";
        String c = "//PROD_NILE/TAB_S2_NA/FLUMEN/Strawberry/EXYNOS5/model/vendor/gts210ltespr/gts210ltespr_apps.mk";
        String d = "//NILE/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/LauncherFacade_Legacy.java";
        String e = "//NILE/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/Utils.java";
        String f = "//DEV/Application/3rdPartyApp/USA/SSL/NILE/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/LauncherFacade_Legacy.java";
        String g = "//DEV/Application/3rdPartyApp/USA/SSL/NILE/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/Utils.java";
        String h = "//PROD_NILE/ZERO_51MR/FLUMEN/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/LauncherFacade_Legacy.java";
        String i = "//PROD_NILE/ZERO_51MR/FLUMEN/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/Utils.java";
        String j = "//PROD_NILE/NOBLE_ZERO2/FLUMEN/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/LauncherFacade_Legacy.java";
        String k = "//PROD_NILE/NOBLE_ZERO2/FLUMEN/Cinnamon/vendor/usa_spr/preload/ID/src/com/sprint/internal/id/Utils.java";
        String l = "//PROD_NILE/KNIGHT/FLUMEN/Cinnamon/frameworks/opt/net/wifi/service/java/com/android/server/wifi/WifiStateMachine.java";
        String m = "//PROD_NILE/KNIGHT/FLUMEN/Cinnamon/frameworks/opt/net/wifi/service/java/com/android/server/wifi/WifiWatchdogStateMachine.java";
        String n = "//PROD_NILE/KNIGHT/FLUMEN/Cinnamon/vendor/usa_spr/frameworks/secsprextension/java/com/sprint/internal/ConnectionManager.java";
        String o = "//DEV/Application/HiddenMenu/L_Branch/res/layout/itson.xml";
        String p = "//DEV/Application/Settings/NILE/Cinnamon/vendor/samsung/packages/apps/SecSettings2/src/com/android/settings/applications/InstalledAppDetails.java";
        String q = "//DEV/Solution/BCDS/MAIN/dummy.txt";
        String r = "//DEV/Solution/Download/TASK/PUBLICAPI/frameworks/base/core/java/android/provider/Downloads.java";
        String t = "";
        String u = "";

        BranchTree<Integer> bt = new PerforceBranchTree<>("root");
        bt.put(a, 1);
        bt.put(b, 2);
        bt.put(c, 3);

        // True
        boolean res = bt.containsPartial("//PROD_NILE/TAB_S2_NA/FLUMEN/");
        System.out.println(" = " + res);
        assert res;

        // False
        res = bt.containsInFull("//PROD_NILE/TAB_S2_NA/FLUMEN/");
        System.out.println(" = " + res);
        assert !res;

        // 1 ~ 3
        List<Integer> cont = new ArrayList<Integer>();
        bt.getAllContainers(cont);
        for (Integer id : cont) {
            System.out.print(", " + id);
        }
        assert cont.size() == 3;

        // 3
        System.out.println();
        cont.clear();
        bt.getAllContainers("//PROD_NILE/TAB_S2_NA/FLUMEN/Strawberry/", cont);
        for (Integer id : cont) {
            System.out.print(", " + id);
        }
        System.out.println();
        assert cont.size() == 1;

        bt.put(d, 4);
        bt.put(e, 5);
        bt.put(f, 6);
        bt.put(g, 7);
        bt.put(h, 8);
        bt.put(i, 9);
        bt.put(j, 10);
        bt.put(k, 11);
        bt.put(l, 12);

        // 1 ~ 12
        cont.clear();
        bt.getAllContainers(cont);
        for (Integer id : cont) {
            System.out.print(", " + id);
        }
        assert cont.size() == 12;

        // True
        System.out.println();
        BranchTree<Integer> bt2 = new PerforceBranchTree<Integer>("root2");
        bt2.put(g, 7);
        bt2.put(h, 8);
        res = isSubTree(bt, bt2);
        System.out.println("result = " + res);
        assert res;

        // False
        bt2 = new PerforceBranchTree<Integer>("root2");
        bt2.put(p, 13);
        bt2.put(q, 14);
        bt2.put(r, 15);
        res = isSubTree(bt, bt2);
        System.out.println("result = " + res);
        assert !res;

        // False
        bt2 = new PerforceBranchTree<Integer>("root2");
        bt2.put(a, 1);
        bt2.put(b, 2);
        bt2.put(r, 3);
        res = isSubTree(bt, bt2);
        System.out.println("result = " + res);
        assert !res;

        // Null
        Integer container = bt.getContainer("//DEV/");
        System.out.println("Result = " + container);
        assert container == null;

        // 6
        container = bt.getContainer(f);
        System.out.println("Result = " + container);
        assert container == 6;

        // Print ALL branch
        System.out.println("QDQWDQWD");
        List<String> str = new ArrayList<String>();
        bt.toString(str, null);
        for (String zx : str) {
            System.out.println(zx);
        }
        assert str.size() == 12;
        assert str.get(0).equals(c);
        assert str.get(1).equals(a);
        assert str.get(2).equals(b);
        assert str.get(3).equals(l);
        assert str.get(4).equals(j);
        assert str.get(5).equals(k);
        assert str.get(6).equals(h);
        assert str.get(7).equals(i);
        assert str.get(8).equals(f);
        assert str.get(9).equals(g);
        assert str.get(10).equals(d);
        assert str.get(11).equals(e);
        str.clear();

        // Print only two
        bt.toStringForSubTree("//NILE/", str, null);
        for (String zx : str) {
            System.out.println(zx);
        }
        assert str.size() == 2;
        assert str.get(0).equals(d);
        assert str.get(1).equals(e);
    }
}
