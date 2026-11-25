package com.reddot.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * 13 套配色。所有界面的颜色都从这里取，切换主题只需要换一个 Palette 对象。
 */
public class Palette {

    public final String id;
    public final String name;
    public final String tag;
    public final String desc;
    public final boolean light;

    public final int accent;
    public final int accent2;
    public final int bg;
    public final int ink;
    public final int sub;
    public final int hint;
    public final int card;
    public final int cardStroke;
    public final int input;
    public final int inputStroke;
    public final int sheet;
    public final int sheetStroke;
    public final int statusBar;
    public final int btnInk;
    public final int danger;
    public final int dot1;
    public final int dot2;
    public final int dot3;
    public final int heroTop;
    public final int heroBottom;
    public final int soft;
    public final int softLine;

    private Palette(B b) {
        id = b.id;
        name = b.name;
        tag = b.tag;
        desc = b.desc;
        light = b.light;
        accent = b.accent;
        accent2 = b.accent2;
        bg = b.bg;
        ink = b.ink;
        sub = b.sub;
        hint = b.hint;
        card = b.card;
        cardStroke = b.cardStroke;
        input = b.input;
        inputStroke = b.inputStroke;
        sheet = b.sheet;
        sheetStroke = b.sheetStroke;
        statusBar = b.statusBar;
        btnInk = b.btnInk;
        danger = b.danger;
        dot1 = b.dot1;
        dot2 = b.dot2;
        dot3 = b.dot3;
        heroTop = b.heroTop;
        heroBottom = b.heroBottom;
        soft = b.soft;
        softLine = b.softLine;
    }

    public String label() {
        return tag.length() == 0 ? name : name + " · " + tag;
    }

    public int accentSoft(int alpha) {
        return (alpha << 24) | (accent & 0x00FFFFFF);
    }

    // ---------------- 主题表 ----------------

    private static final List<Palette> ALL = new ArrayList<>();

    static {
        ALL.add(new B("neon", "霓虹红")
                .t("默认", "深色底 + 霓虹红发光")
                .ac(0xFFFF2D55, 0xFFFF6B3D)
                .bg(0xFF08080C).ink(0xFFF2F2F5, 0xFF9A9AA6, 0xFF5E5E6B)
                .card(0xFF16161D, 0x26FFFFFF)
                .input(0xFF12121A, 0xFF26262F, 0xFF1A1A22)
                .hero(0xFF1E1019)
                .dots(0xFFFF7583, 0xFFFF2D55, 0xFFB0123A)
                .build());

        ALL.add(new B("cyan", "赛博青")
                .t("", "冷调科技感，看久了更冷静")
                .ac(0xFF22D3EE, 0xFF3B82F6)
                .bg(0xFF05080D).ink(0xFFEAF6FA, 0xFF8FA6B2, 0xFF546B76)
                .card(0xFF121C22, 0x26FFFFFF)
                .input(0xFF0B141B, 0xFF1C303B, 0xFF0E1A22)
                .hero(0xFF0A1E28)
                .dots(0xFF8DF0FB, 0xFF22D3EE, 0xFF0A5E74)
                .build());

        ALL.add(new B("violet", "暮光紫")
                .t("", "夜读友好，紫粉渐变")
                .ac(0xFFA855F7, 0xFFEC4899)
                .bg(0xFF07050D).ink(0xFFF4EFFB, 0xFFA196B8, 0xFF655A7A)
                .card(0xFF161022, 0x26FFFFFF)
                .input(0xFF130E1E, 0xFF2A2038, 0xFF181122)
                .hero(0xFF1C0E28)
                .dots(0xFFD8B4FE, 0xFFA855F7, 0xFF5B189B)
                .build());

        ALL.add(new B("emerald", "极光绿")
                .t("", "清爽护眼，绿松石渐变")
                .ac(0xFF34D399, 0xFF06B6D4)
                .bg(0xFF050A08).ink(0xFFEAF7F1, 0xFF8FA9A0, 0xFF536B62)
                .card(0xFF101A16, 0x26FFFFFF)
                .input(0xFF0A1411, 0xFF1D332C, 0xFF0E1A16)
                .hero(0xFF0A1E18)
                .dots(0xFF9AF6D2, 0xFF34D399, 0xFF0A6B4F)
                .build());

        ALL.add(new B("vapor", "蒸汽波")
                .t("", "粉紫配青蓝，90 年代霓虹")
                .ac(0xFFFF71CE, 0xFF01CDFE)
                .bg(0xFF0D0618).ink(0xFFF8EEFF, 0xFFB49FCE, 0xFF6F5B8A)
                .card(0xFF1A1029, 0x26FFFFFF)
                .input(0xFF170C28, 0xFF33204D, 0xFF1C102C)
                .hero(0xFF200C30)
                .dots(0xFFFFB3E6, 0xFFFF71CE, 0xFF8A1E6B)
                .build());

        ALL.add(new B("hazard", "警示黄")
                .t("", "黑黄配色，最扎眼的一套")
                .ac(0xFFFFD60A, 0xFFFF9F0A)
                .bg(0xFF0A0A0A).ink(0xFFF7F7F2, 0xFFA6A69C, 0xFF5E5E56)
                .card(0xFF181812, 0x26FFFFFF)
                .input(0xFF141410, 0xFF2C2C22, 0xFF181812)
                .hero(0xFF1C1A0A)
                .dots(0xFFFFF08A, 0xFFFFD60A, 0xFF8A6E00)
                .btnInk(0xFF1A1600)
                .build());

        ALL.add(new B("amber", "复古琥珀")
                .t("CRT", "老显像管的暖橙单色")
                .ac(0xFFFFB000, 0xFFFF7A00)
                .bg(0xFF120C06).ink(0xFFF8EDD9, 0xFFB7A07C, 0xFF6E5B44)
                .card(0xFF1E1509, 0x26FFD696)
                .input(0xFF1B1208, 0xFF3A2A12, 0xFF22170B)
                .hero(0xFF2A1A0A)
                .dots(0xFFFFDE9C, 0xFFFFB000, 0xFF8A4E00)
                .btnInk(0xFF241300)
                .build());

        ALL.add(new B("olive", "军绿")
                .t("", "军绿底 + 荧光青柠")
                .ac(0xFFC4F042, 0xFF7FB800)
                .bg(0xFF0B0D07).ink(0xFFF1F5E4, 0xFFA9B18F, 0xFF66704F)
                .card(0xFF151B0C, 0x26FFFFFF)
                .input(0xFF12160B, 0xFF2A3318, 0xFF151B0C)
                .hero(0xFF1A220C)
                .dots(0xFFE4FF9E, 0xFFC4F042, 0xFF5E7A00)
                .btnInk(0xFF17200A)
                .build());

        ALL.add(new B("abyss", "深海")
                .t("", "藏青底 + 冰蓝到靛蓝")
                .ac(0xFF38BDF8, 0xFF6366F1)
                .bg(0xFF04080F).ink(0xFFE8F3FA, 0xFF8FA6B8, 0xFF55707F)
                .card(0xFF0C1A26, 0x26FFFFFF)
                .input(0xFF0A1622, 0xFF17303F, 0xFF0C1A26)
                .hero(0xFF081A28)
                .dots(0xFF9FE4FF, 0xFF38BDF8, 0xFF0B5F82)
                .build());

        ALL.add(new B("blueprint", "蓝图")
                .t("工程", "工程图纸质感，网格更明显")
                .ac(0xFF4FC3F7, 0xFF7DD3FC)
                .bg(0xFF061829).ink(0xFFDDEBFA, 0xFF8AA8C4, 0xFF4E6C88)
                .card(0xFF0A2338, 0x38FFFFFF)
                .input(0xFF0A2338, 0xFF17384F, 0xFF0A2032)
                .hero(0xFF092236)
                .dots(0xFFA5E4FF, 0xFF4FC3F7, 0xFF0A4A6B)
                .build());

        ALL.add(new B("sakura", "樱花粉")
                .t("浅色", "奶油粉底，柔和到不刺眼")
                .light()
                .ac(0xFFFF6F91, 0xFFFF9EB5)
                .bg(0xFFFFF7F8).ink(0xFF2A2024, 0xFF8A7079, 0xFFC2A9B1)
                .card(0xFFFFFFFF, 0x1A502837)
                .input(0xFFFBEEF1, 0x1F502837, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFFFFD1DC, 0xFFFF6F91, 0xFFA83254)
                .status(0xFF4A3540)
                .danger(0xFFD64550)
                .build());

        ALL.add(new B("paper", "墨水屏")
                .t("单色", "纸感灰阶，几乎无彩色")
                .light()
                .ac(0xFF2A2A2A, 0xFF6A6A62)
                .bg(0xFFEDEBE3).ink(0xFF1A1A18, 0xFF5E5E58, 0xFF9A9A92)
                .card(0xFFFBFAF5, 0x1F141410)
                .input(0xFFE4E2D9, 0x24141410, 0xFFFBFAF5)
                .hero(0xFFFBFAF5)
                .dots(0xFF8C8C84, 0xFF3A3A36, 0xFF1A1A18)
                .status(0xFF26261F)
                .btnInk(0xFFFBFAF5)
                .danger(0xFF8A2A20)
                .build());

        ALL.add(new B("classic", "红白极简")
                .t("浅色", "白底 + 经典红，白天更清楚")
                .light()
                .ac(0xFFE5342F, 0xFFFF6B3D)
                .bg(0xFFF5F5F8).ink(0xFF15151B, 0xFF6B6B78, 0xFFA3A3AF)
                .card(0xFFFFFFFF, 0x14141428)
                .input(0xFFF2F2F6, 0x17141428, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFFFF8A80, 0xFFE5342F, 0xFFA81E1A)
                .status(0xFF23232B)
                .danger(0xFFD32F2F)
                .build());

        // ---------------- 浅色 ----------------

        ALL.add(new B("cream", "奶油白")
                .t("浅色", "暖白底 + 焦糖橙，白天最省眼")
                .light()
                .ac(0xFFC4702A, 0xFFE0894A)
                .bg(0xFFFDF8F0).ink(0xFF2B231C, 0xFF7A6A5C, 0xFFB3A493)
                .card(0xFFFFFFFF, 0x1A3A2A10)
                .input(0xFFF6EDE1, 0x1F3A2A10, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFFF3C79A, 0xFFC4702A, 0xFF8A4A12)
                .status(0xFF3A2A18)
                .btnInk(0xFFFFFFFF)
                .danger(0xFFC0392B)
                .build());

        ALL.add(new B("mint", "薄荷")
                .t("浅色", "浅薄荷底 + 深绿，清爽不刺眼")
                .light()
                .ac(0xFF0B8F63, 0xFF22C08A)
                .bg(0xFFF2FBF6).ink(0xFF16241E, 0xFF5F7A6E, 0xFF9DB5A8)
                .card(0xFFFFFFFF, 0x1A0E3A2A)
                .input(0xFFE9F6EF, 0x1F0E3A2A, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFFA7E8CE, 0xFF0B8F63, 0xFF08604F)
                .status(0xFF1C3A2E)
                .btnInk(0xFFFFFFFF)
                .danger(0xFFC2410C)
                .build());

        ALL.add(new B("mist", "雾蓝")
                .t("浅色", "灰蓝底 + 皇家蓝，冷静的工作色")
                .light()
                .ac(0xFF2563EB, 0xFF3B82F6)
                .bg(0xFFF4F7FC).ink(0xFF161C2B, 0xFF5D6B85, 0xFF9AA8C0)
                .card(0xFFFFFFFF, 0x1A16264A)
                .input(0xFFEAF0FA, 0x1F16264A, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFFA9C4F5, 0xFF2563EB, 0xFF123A7A)
                .status(0xFF1B2438)
                .btnInk(0xFFFFFFFF)
                .danger(0xFFDC2626)
                .build());

        ALL.add(new B("clay", "豆沙")
                .t("浅色", "米杏底 + 陶土红，柔和的纸感")
                .light()
                .ac(0xFFA8452F, 0xFFC96A4E)
                .bg(0xFFFAF3EF).ink(0xFF2A201C, 0xFF7A6459, 0xFFB5A096)
                .card(0xFFFFFFFF, 0x1A3A2018)
                .input(0xFFF5E9E2, 0x1F3A2018, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFFE8BFAB, 0xFFA8452F, 0xFF6E2C1E)
                .status(0xFF3A2A22)
                .btnInk(0xFFFFFFFF)
                .danger(0xFFB91C1C)
                .build());

        // ---------------- 撞色 ----------------

        ALL.add(new B("klein", "克莱因蓝 × 橙")
                .t("撞色", "蓝底按钮配橙标，对比拉满")
                .light()
                .ac(0xFF2440D8, 0xFFFF6B1A)
                .bg(0xFFF7F8FF).ink(0xFF12142B, 0xFF5A5F80, 0xFF9BA0BE)
                .card(0xFFFFFFFF, 0x1A1A2B6A)
                .input(0xFFEEF0FF, 0x1F1A2B6A, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFFFFB27A, 0xFF2440D8, 0xFF16309E)
                .status(0xFF171A3A)
                .btnInk(0xFFFFFFFF)
                .danger(0xFFD92D20)
                .build());

        ALL.add(new B("clash", "玫红 × 薄荷")
                .t("撞色", "红绿互补，跳但看得清")
                .light()
                .ac(0xFFD21845, 0xFF10B981)
                .bg(0xFFFBF7F7).ink(0xFF1E1418, 0xFF6E5560, 0xFFAC98A2)
                .card(0xFFFFFFFF, 0x1A3A1620)
                .input(0xFFF7ECEF, 0x1F3A1620, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFF7DD3B0, 0xFFD21845, 0xFF8A1030)
                .status(0xFF2A1620)
                .btnInk(0xFFFFFFFF)
                .danger(0xFFB91C1C)
                .build());

        ALL.add(new B("citrus", "柠檬紫")
                .t("撞色", "紫配琥珀，浅底也够精神")
                .light()
                .ac(0xFF6D28D9, 0xFFF59E0B)
                .bg(0xFFFCFAF2).ink(0xFF221C2E, 0xFF6A607A, 0xFFA79CB8)
                .card(0xFFFFFFFF, 0x1A2E1F4A)
                .input(0xFFF6F1FF, 0x1F2E1F4A, 0xFFFFFFFF)
                .hero(0xFFFFFFFF)
                .dots(0xFFF7D08A, 0xFF6D28D9, 0xFF44188F)
                .status(0xFF2A2140)
                .btnInk(0xFFFFFFFF)
                .danger(0xFFDC2626)
                .build());
    }

    private static class B {
        final String id;
        final String name;
        String tag = "";
        String desc = "";
        boolean light = false;
        int accent = 0xFFFF2D55;
        int accent2 = 0xFFFF6B3D;
        int bg = 0xFF08080C;
        int ink = 0xFFF2F2F5;
        int sub = 0xFF9A9AA6;
        int hint = 0xFF5E5E6B;
        int card = 0xFF16161D;
        int cardStroke = 0x26FFFFFF;
        int input = 0xFF12121A;
        int inputStroke = 0xFF26262F;
        int sheet = 0xFF1A1A22;
        int sheetStroke = 0x1AFFFFFF;
        int statusBar = 0xFFFFFFFF;
        int btnInk = 0xFFFFFFFF;
        int danger = 0xFFFF5C72;
        int dot1 = 0xFFFF7583;
        int dot2 = 0xFFFF2D55;
        int dot3 = 0xFFB0123A;
        int heroTop = 0xFF1E1019;
        int heroBottom = -1;
        int soft = 0x14FFFFFF;
        int softLine = 0x26FFFFFF;

        B(String id, String name) {
            this.id = id;
            this.name = name;
        }

        B t(String tag, String desc) {
            this.tag = tag;
            this.desc = desc;
            return this;
        }

        B light() {
            light = true;
            soft = 0x0F000000;
            softLine = 0x1F000000;
            return this;
        }

        B ac(int a, int b) {
            accent = a;
            accent2 = b;
            return this;
        }

        B bg(int c) {
            bg = c;
            return this;
        }

        B ink(int a, int s, int h) {
            ink = a;
            sub = s;
            hint = h;
            return this;
        }

        B card(int c, int stroke) {
            card = c;
            cardStroke = stroke;
            return this;
        }

        B input(int c, int stroke, int sh) {
            input = c;
            inputStroke = stroke;
            sheet = sh;
            return this;
        }

        B hero(int top) {
            heroTop = top;
            return this;
        }

        B dots(int a, int b, int c) {
            dot1 = a;
            dot2 = b;
            dot3 = c;
            return this;
        }

        B btnInk(int c) {
            btnInk = c;
            return this;
        }

        B status(int c) {
            statusBar = c;
            return this;
        }

        B danger(int c) {
            danger = c;
            return this;
        }

        Palette build() {
            heroBottom = bg;
            if (light) sheetStroke = 0x1A000000;
            return new Palette(this);
        }
    }

    // ---------------- 读写 ----------------

    public static final String K_THEME = "theme";
    private static final String K_RECENT = "recent_themes";
    private static final int RECENT_MAX = 5;

    public static List<Palette> all() {
        return ALL;
    }

    public static Palette byIdOrNull(String id) {
        if (id == null) return null;
        for (Palette p : ALL) if (p.id.equals(id)) return p;
        return null;
    }

    public static Palette byId(String id) {
        Palette p = byIdOrNull(id);
        return p != null ? p : byIdOrNull("cream");
    }

    public static Palette get(Context c) {
        return byId(TodoStore.sp(c).getString(K_THEME, "cream"));
    }

    public static void set(Context c, String id) {
        TodoStore.sp(c).edit().putString(K_THEME, id).apply();
        touchRecent(c, id);
    }

    private static void touchRecent(Context c, String id) {
        List<String> list = recentIds(c);
        list.remove(id);
        list.add(0, id);
        while (list.size() > RECENT_MAX) list.remove(list.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append(',');
            sb.append(s);
        }
        TodoStore.sp(c).edit().putString(K_RECENT, sb.toString()).apply();
    }

    public static List<String> recentIds(Context c) {
        List<String> out = new ArrayList<>();
        String raw = TodoStore.sp(c).getString(K_RECENT, "");
        if (raw == null || raw.length() == 0) return out;
        for (String s : raw.split(",")) {
            if (s.length() > 0 && byIdOrNull(s) != null && !out.contains(s)) out.add(s);
        }
        return out;
    }

    /** 当前主题 → 最近用过 → 没用过的浅色 → 没用过的深色 */
    public static List<Palette> allSorted(Context c) {
        List<Palette> out = new ArrayList<>();
        Palette cur = get(c);
        if (cur != null) out.add(cur);
        for (String id : recentIds(c)) {
            Palette p = byIdOrNull(id);
            if (p != null && !out.contains(p)) out.add(p);
        }
        for (Palette p : ALL) {
            if (p.light && !out.contains(p)) out.add(p);
        }
        for (Palette p : ALL) {
            if (!p.light && !out.contains(p)) out.add(p);
        }
        return out;
    }
}
