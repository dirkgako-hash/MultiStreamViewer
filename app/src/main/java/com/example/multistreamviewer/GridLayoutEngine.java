package com.example.multistreamviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Motor de grelha por presets (árvore guillotine), portado do protótipo
 * desktop (ui/modules/LayoutManager.js): folha = índice da box visível,
 * nó = divisão 'v' (lado a lado) ou 'h' (empilhada). Pesos por divisão são
 * arrastáveis e persistidos via WeightsStore (chave: presetId + caminho do nó).
 */
public class GridLayoutEngine {

    public interface WeightsStore {
        /** Pesos normalizados do nó, ou null para divisão uniforme. */
        float[] get(String presetId, String path, int count);
        void save(String presetId, String path, float[] weights);
    }

    public static final class Node {
        public final int leaf;          // >= 0 → folha
        public final char dir;          // 'v' lado a lado, 'h' empilhado
        public final List<Node> kids;

        private Node(int leaf) { this.leaf = leaf; this.dir = 0; this.kids = null; }
        private Node(char dir, List<Node> kids) { this.leaf = -1; this.dir = dir; this.kids = kids; }
    }

    public static final class Preset {
        public final String id, label;
        public final int n;
        public final Node tree;

        Preset(String id, String label, Node tree) {
            this.id = id; this.label = label; this.tree = tree; this.n = countLeaves(tree);
        }
    }

    // ── construtores da árvore ────────────────────────────────────────────────
    private static Node leaf(int i) { return new Node(i); }

    private static Node side(List<Node> l) { return l.size() == 1 ? l.get(0) : new Node('v', l); }
    private static Node pile(List<Node> l) { return l.size() == 1 ? l.get(0) : new Node('h', l); }

    private static Node side(Node... k) { return side(Arrays.asList(k)); }
    private static Node pile(Node... k) { return pile(Arrays.asList(k)); }

    private static List<Node> leaves(int from, int to) {
        List<Node> l = new ArrayList<>();
        for (int i = from; i < to; i++) l.add(leaf(i));
        return l;
    }

    public static int countLeaves(Node n) {
        if (n.leaf >= 0) return 1;
        int c = 0;
        for (Node k : n.kids) c += countLeaves(k);
        return c;
    }

    /** Mosaico: 2 colunas grandes, esquerda com metade das boxes, direita com o resto. */
    private static Node mosaico(int from, int to) {
        int h = (to - from) / 2;
        return side(pile(leaves(from, from + h)), pile(leaves(from + h, to)));
    }

    /** Grade R×C uniforme. */
    private static Node grade(int n, int rows, int cols) {
        List<Node> bands = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            bands.add(side(leaves(r * cols, Math.min(n, (r + 1) * cols))));
        return pile(bands);
    }

    /** Box 0 ocupa uma banda/coluna inteira; as restantes em mosaico. */
    private static Node grande(String where, int n) {
        Node rest = mosaico(1, n);
        switch (where) {
            case "topo":  return pile(Arrays.asList(leaf(0), rest));
            case "fundo": return pile(Arrays.asList(rest, leaf(0)));
            case "esq":   return side(Arrays.asList(leaf(0), rest));
            default:      return side(Arrays.asList(rest, leaf(0)));
        }
    }

    // ── catálogo de presets ───────────────────────────────────────────────────
    private static final List<Preset> PRESETS = new ArrayList<>();

    private static void add(String id, String label, Node tree) { PRESETS.add(new Preset(id, label, tree)); }

    static {
        add("1-full", "Preenchida", leaf(0));
        add("2-lado", "Lado a lado", side(leaf(0), leaf(1)));
        add("2-pilha", "Empilhadas", pile(leaf(0), leaf(1)));
        add("3-linha", "3 em linha", side(leaf(0), leaf(1), leaf(2)));
        add("3-pilha", "3 empilhadas", pile(leaf(0), leaf(1), leaf(2)));
        add("3-grande-esq", "Grande à esquerda", side(leaf(0), pile(leaf(1), leaf(2))));
        add("3-grande-dir", "Grande à direita", side(pile(leaf(0), leaf(1)), leaf(2)));
        add("4-mosaico", "Mosaico 2 colunas", mosaico(0, 4));
        add("4-grade", "Grade 2x2", grade(4, 2, 2));
        add("4-t31", "3 em cima + 1 grande", pile(side(leaves(0, 3)), leaf(3)));
        add("4-t13", "1 grande + 3 em baixo", pile(leaf(0), side(leaves(1, 4))));
        add("4-l31", "3 à esquerda + 1 grande", side(pile(leaves(0, 3)), leaf(3)));
        add("4-l13", "1 grande + 3 à direita", side(leaf(0), pile(leaves(1, 4))));
        add("5-mosaico", "Mosaico 2+3", side(pile(leaves(0, 2)), pile(leaves(2, 5))));
        add("5-t32", "3 em cima + 2 em baixo", pile(side(leaves(0, 3)), side(leaves(3, 5))));
        add("5-t23", "2 em cima + 3 em baixo", pile(side(leaves(0, 2)), side(leaves(2, 5))));
        add("5-l32", "3 à esquerda + 2 à direita", side(pile(leaves(0, 3)), pile(leaves(3, 5))));
        add("6-mosaico", "Mosaico 3+3", side(pile(leaves(0, 3)), pile(leaves(3, 6))));
        add("6-grade-2x3", "Grade 2x3", grade(6, 2, 3));
        add("6-grade-3x2", "Grade 3x2", grade(6, 3, 2));
        for (int n = 4; n <= 6; n++) {
            add(n + "-pilha", n + " empilhadas", pile(leaves(0, n)));
            for (String w : new String[]{"topo", "fundo", "esq", "dir"})
                add(n + "-grande-" + w, "Grande " + w, grande(w, n));
        }
    }

    public static List<Preset> presetsFor(int n) {
        List<Preset> out = new ArrayList<>();
        for (Preset p : PRESETS) if (p.n == n) out.add(p);
        return out;
    }

    public static Preset byId(String id) {
        if (id == null) return null;
        for (Preset p : PRESETS) if (p.id.equals(id)) return p;
        return null;
    }

    public static String defaultPresetId(int n, boolean portrait) {
        if (portrait) {
            if (n == 1) return "1-full";
            String stack = n + "-pilha";
            if (byId(stack) != null) return stack;
        }
        String[] pref = {"1-full", "2-lado", "3-linha", "4-mosaico", "5-mosaico", "6-mosaico"};
        if (n >= 1 && n <= 6 && byId(pref[n - 1]) != null) return pref[n - 1];
        List<Preset> l = presetsFor(n);
        return l.isEmpty() ? "1-full" : l.get(0).id;
    }

    // ── construção da vista ───────────────────────────────────────────────────
    private final Context ctx;
    private final int divPx;
    private final int minPx;

    public GridLayoutEngine(Context ctx, int dividerPx, int minCellPx) {
        this.ctx = ctx;
        this.divPx = dividerPx;
        this.minPx = minCellPx;
    }

    /** @param visible containers das boxes ativas, por ordem de leitura da árvore. */
    public View build(Preset preset, List<View> visible, WeightsStore store) {
        if (preset.n != visible.size()) {
            preset = byId(defaultPresetId(visible.size(), false));
        }
        if (preset == null || preset.n == 1) return visible.get(0);
        return walk(preset, preset.tree, visible, "r", store);
    }

    private View walk(Preset preset, Node node, List<View> visible, String path, WeightsStore store) {
        if (node.leaf >= 0) return visible.get(node.leaf);
        final int k = node.kids.size();
        final boolean horiz = node.dir == 'v';
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(horiz ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        float[] w = store.get(preset.id, path, k);
        if (w == null || w.length != k) {
            w = new float[k];
            Arrays.fill(w, 1f / k);
        }

        for (int i = 0; i < k; i++) {
            if (i > 0) {
                View div = makeDivider(horiz);
                div.setOnTouchListener(dividerTouch(ll, i, path, preset.id, store));
                ll.addView(div);
            }
            View child = walk(preset, node.kids.get(i), visible, path + "." + i, store);
            LinearLayout.LayoutParams lp = horiz
                    ? new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, w[i])
                    : new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, w[i]);
            ll.addView(child, lp);
        }
        return ll;
    }

    private View makeDivider(boolean horizontal) {
        View v = new View(ctx);
        v.setLayoutParams(horizontal
                ? new LinearLayout.LayoutParams(divPx, LinearLayout.LayoutParams.MATCH_PARENT)
                : new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, divPx));
        v.setBackgroundColor(android.graphics.Color.parseColor("#555555"));
        return v;
    }

    /**
     * Arraste da fronteira `boundary` (entre os filhos i-1 e i do nó): só os
     * dois pesos adjacentes mudam; em UP/CANCEL os pesos do nó são persistidos.
     */
    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener dividerTouch(final LinearLayout parent, final int boundary,
                                              final String path, final String presetId,
                                              final WeightsStore store) {
        final int cellA = 2 * (boundary - 1);
        final int cellB = 2 * boundary - 1;
        final float[] startYX = new float[1];
        final float[] startW = new float[2];
        final int[] startPx = new int[2];

        return (v, ev) -> {
            View a = parent.getChildAt(cellA);
            View b = parent.getChildAt(cellB);
            if (!(a.getLayoutParams() instanceof LinearLayout.LayoutParams)
                    || !(b.getLayoutParams() instanceof LinearLayout.LayoutParams)) return false;
            final LinearLayout.LayoutParams pa = (LinearLayout.LayoutParams) a.getLayoutParams();
            final LinearLayout.LayoutParams pb = (LinearLayout.LayoutParams) b.getLayoutParams();
            final boolean horiz = parent.getOrientation() == LinearLayout.HORIZONTAL;

            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    startYX[0] = horiz ? ev.getRawX() : ev.getRawY();
                    startW[0] = pa.weight;
                    startW[1] = pb.weight;
                    startPx[0] = horiz ? a.getWidth() : a.getHeight();
                    startPx[1] = horiz ? b.getWidth() : b.getHeight();
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    int pxTotal = startPx[0] + startPx[1];
                    float room = startW[0] + startW[1];
                    if (pxTotal <= 0 || room <= 0) return true;
                    float d = (horiz ? ev.getRawX() : ev.getRawY()) - startYX[0];
                    float dw = d * room / pxTotal;
                    float minW = room * minPx / (float) pxTotal;
                    float newA = Math.max(minW, Math.min(room - minW, startW[0] + dw));
                    pa.weight = newA;
                    pb.weight = room - newA;
                    a.setLayoutParams(pa);
                    b.setLayoutParams(pb);
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    List<Float> ws = new ArrayList<>();
                    float sum = 0;
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        LinearLayout.LayoutParams p =
                                (LinearLayout.LayoutParams) parent.getChildAt(i).getLayoutParams();
                        if (p.weight > 0) { ws.add(p.weight); sum += p.weight; }
                    }
                    if (sum > 0) {
                        float[] norm = new float[ws.size()];
                        for (int i = 0; i < norm.length; i++) norm[i] = ws.get(i) / sum;
                        store.save(presetId, path, norm);
                    }
                    return true;
                }
                default:
                    return false;
            }
        };
    }
}
