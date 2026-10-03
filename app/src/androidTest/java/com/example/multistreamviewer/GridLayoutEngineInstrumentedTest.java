package com.example.multistreamviewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testes de instrumentação do motor de grelha: exercitam o arraste real duma
 * separadora (MotionEvents) sobre uma vista medida e layoutada, porque o bug do
 * resize só corrompia pesos gravados a partir do ecrã.
 *
 * Correr com aparelho/TV ligado:  ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class GridLayoutEngineInstrumentedTest {

    private static final float EPS = 1e-4f;
    private static final int DIV_PX = 12;
    private static final int MIN_PX = 40;
    private static final int WIDTH_PX = 1200;
    private static final int HEIGHT_PX = 400;

    private static Context ctx() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /** Guarda o que foi gravado e devolve o que se quiser no próximo build. */
    private static final class RecordingStore implements GridLayoutEngine.WeightsStore {
        final Map<String, float[]> saved = new HashMap<>();
        final Map<String, float[]> toReturn = new HashMap<>();

        @Override
        public float[] get(String presetId, String path, int count) {
            float[] w = toReturn.get(presetId + "/" + path);
            return w != null && w.length == count ? w : null;
        }

        @Override
        public void save(String presetId, String path, float[] weights) {
            saved.put(presetId + "/" + path, weights);
        }
    }

    private static List<View> dummyCells(int n) {
        List<View> cells = new ArrayList<>();
        for (int i = 0; i < n; i++) cells.add(new View(ctx()));
        return cells;
    }

    /** Constrói o preset e dá-lhe tamanho real, para os pesos virarem pixels. */
    private static LinearLayout buildLaidOut(String presetId, GridLayoutEngine engine,
                                             GridLayoutEngine.WeightsStore store) {
        GridLayoutEngine.Preset preset = GridLayoutEngine.byId(presetId);
        LinearLayout root = (LinearLayout) engine.build(preset, dummyCells(preset.n), store);
        root.measure(View.MeasureSpec.makeMeasureSpec(WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT_PX, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, WIDTH_PX, HEIGHT_PX);
        return root;
    }

    private static float weight(View v) {
        return ((LinearLayout.LayoutParams) v.getLayoutParams()).weight;
    }

    /**
     * Prensar uma separadora e arrastá-la `dist` px ao longo do eixo do nó, depois
     * relayoutar o nó: `setLayoutParams` faz `requestLayout`, mas um view tree sem
     * janela (o teste monta o root à mão) não agendamento nenhum traversal, por isso
     * os pixels só se movem quando o teste força a passagem de layout.
     */
    private static void drag(View divider, boolean horizontal, float dist) {
        long t = SystemClock.uptimeMillis();
        float x = 200f, y = 200f;
        float moved = x + dist;
        divider.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0));
        divider.dispatchTouchEvent(MotionEvent.obtain(t, t + 10, MotionEvent.ACTION_MOVE,
                horizontal ? moved : x, horizontal ? y : y + dist, 0));
        divider.dispatchTouchEvent(MotionEvent.obtain(t, t + 20, MotionEvent.ACTION_UP,
                horizontal ? moved : x, horizontal ? y : y + dist, 0));
        relayout((View) divider.getParent());
    }

    /** Re-mele e re-arranja um nó no mesmo sítio e tamanho, como faria a janela. */
    private static void relayout(View node) {
        node.measure(View.MeasureSpec.makeMeasureSpec(node.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(node.getHeight(), View.MeasureSpec.EXACTLY));
        node.layout(node.getLeft(), node.getTop(),
                node.getLeft() + node.getWidth(), node.getTop() + node.getHeight());
    }

    private static void assertSomaUm(float[] w) {
        float s = 0;
        for (float f : w) s += f;
        assertEquals(1f, s, 1e-5f);
    }

    @Test
    public void tresEmLinhaTemTresCelulasEduasSeparadoras() {
        LinearLayout root = buildLaidOut("3-linha", new GridLayoutEngine(ctx(), DIV_PX, MIN_PX),
                new RecordingStore());
        assertEquals(5, root.getChildCount());
        for (int i = 0; i < 3; i++) {
            assertTrue("célula " + i + " sem peso", weight(root.getChildAt(2 * i)) > 0f);
            int w = root.getChildAt(2 * i).getWidth();
            assertTrue("célula " + i + " com " + w + "px", w > 380 && w < 400);
        }
        assertEquals("separadora não pode ter peso", 0f, weight(root.getChildAt(1)), EPS);
        assertEquals("separadora não pode ter peso", 0f, weight(root.getChildAt(3)), EPS);
        assertEquals(DIV_PX, root.getChildAt(1).getWidth());
        assertEquals(DIV_PX, root.getChildAt(3).getWidth());
    }

    /** Regressão do bug: o arraste actua as duas células vizinhas, nunca a separadora. */
    @Test
    public void arrastarSeparadoraMudaAsCelulasVizinhas() {
        LinearLayout root = buildLaidOut("3-linha", new GridLayoutEngine(ctx(), DIV_PX, MIN_PX),
                new RecordingStore());
        View cell0 = root.getChildAt(0), divider = root.getChildAt(1), cell1 = root.getChildAt(2);
        int w0 = cell0.getWidth(), w1 = cell1.getWidth();

        drag(divider, true, 200f);

        assertTrue("a célula à esquerda não cresceu", cell0.getWidth() > w0);
        assertTrue("a célula à direita não encolheu", cell1.getWidth() < w1);
        assertEquals("a separadora ganhou peso — o bug do resize", 0f, weight(divider), EPS);
        assertEquals("a separadora mudou de largura", DIV_PX, divider.getWidth());
        // O LinearLayout reparte pixels inteiros: a fronteira pode render ±1 px às
        // células vizinhas ao arredondar, mas o par da fronteira não ganha nem perde espaço.
        int antes = w0 + w1;
        int depois = cell0.getWidth() + cell1.getWidth();
        assertTrue("as duas células da fronteira mudaram de total (" + antes + " → " + depois + ")",
                Math.abs(antes - depois) <= 1);
    }

    /** O que fica no store tem de ter exactamente k entradas, senão get(…, k) rejeita. */
    @Test
    public void arrastarGravaUmPesoPorCelula() {
        RecordingStore store = new RecordingStore();
        LinearLayout root = buildLaidOut("3-linha", new GridLayoutEngine(ctx(), DIV_PX, MIN_PX), store);

        drag(root.getChildAt(1), true, 120f);

        assertTrue("nada foi gravado", store.saved.containsKey("3-linha/r"));
        float[] w = store.saved.get("3-linha/r");
        assertEquals(3, w.length);
        assertSomaUm(w);
        assertTrue("arrastar para a direita dá mais peso à esquerda", w[0] > w[1]);
        assertEquals("célula longe da fronteira não muda", 1f / 3f, w[2], 0.02f);
    }

    /** Reconstruir com o que foi gravado tem de repor exactamente as proporções. */
    @Test
    public void pesosGravadosReconstroemAGrelha() {
        GridLayoutEngine engine = new GridLayoutEngine(ctx(), DIV_PX, MIN_PX);
        RecordingStore store = new RecordingStore();
        LinearLayout root = buildLaidOut("3-linha", engine, store);
        drag(root.getChildAt(1), true, 160f);
        float[] gravados = store.saved.get("3-linha/r");

        RecordingStore relido = new RecordingStore();
        relido.toReturn.put("3-linha/r", gravados);
        LinearLayout novo = buildLaidOut("3-linha", engine, relido);

        for (int i = 0; i < 3; i++) {
            assertEquals("célula " + i + " não reconstituída",
                    gravados[i], weight(novo.getChildAt(2 * i)), EPS);
        }
        assertEquals("a separadora voltou a comer espaço", DIV_PX, novo.getChildAt(1).getWidth());
    }

    @Test
    public void divisaoVerticalReparteAlturas() {
        RecordingStore store = new RecordingStore();
        LinearLayout root = buildLaidOut("3-pilha", new GridLayoutEngine(ctx(), DIV_PX, MIN_PX), store);
        assertEquals(LinearLayout.VERTICAL, root.getOrientation());
        View top = root.getChildAt(0), divider = root.getChildAt(1), mid = root.getChildAt(2);
        int h0 = top.getHeight(), h1 = mid.getHeight();
        assertEquals(DIV_PX, divider.getHeight());
        assertEquals(WIDTH_PX, top.getWidth());

        drag(divider, false, 60f);

        assertTrue("a célula de cima não cresceu", top.getHeight() > h0);
        assertTrue("a célula do meio não encolheu", mid.getHeight() < h1);
        assertEquals("a separadora ganhou peso", 0f, weight(divider), EPS);
        float[] w = store.saved.get("3-pilha/r");
        assertEquals(3, w.length);
        assertTrue(w[0] > w[1]);
    }

    /** Modo TV: sem arraste, a separadora não reage aos toques e nada é gravado. */
    @Test
    public void fireTvNaoArrasta() {
        GridLayoutEngine engine = new GridLayoutEngine(ctx(), DIV_PX, MIN_PX);
        engine.resizable = false;
        RecordingStore store = new RecordingStore();
        LinearLayout root = buildLaidOut("3-linha", engine, store);
        View cell0 = root.getChildAt(0), divider = root.getChildAt(1);
        int w0 = cell0.getWidth();

        drag(divider, true, 300f);

        assertEquals("arrastou com resizable=false", w0, cell0.getWidth());
        assertTrue("gravou pesos com resizable=false", store.saved.isEmpty());
    }

    /** O limite prensado fica vermelho e volta ao normal ao largar. */
    @Test
    public void limitePrensadoFicaVermelho() {
        LinearLayout root = buildLaidOut("3-linha", new GridLayoutEngine(ctx(), DIV_PX, MIN_PX),
                new RecordingStore());
        View divider = root.getChildAt(1);
        int normal = divider.getBackground() == null ? 0
                : ((android.graphics.drawable.ColorDrawable) divider.getBackground()).getColor();

        long t = SystemClock.uptimeMillis();
        divider.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 200f, 200f, 0));
        int ativo = ((android.graphics.drawable.ColorDrawable) divider.getBackground()).getColor();
        divider.dispatchTouchEvent(MotionEvent.obtain(t, t + 10, MotionEvent.ACTION_UP, 200f, 200f, 0));
        int depois = ((android.graphics.drawable.ColorDrawable) divider.getBackground()).getColor();

        assertTrue("o limite não ficou vermelho ao prensar",
                android.graphics.Color.red(ativo) > android.graphics.Color.green(ativo)
                        && android.graphics.Color.red(ativo) > 0x80);
        assertEquals("o limite não voltou à cor normal", normal, depois);
    }

    /** Cada nó persiste pelo seu caminho: arrastar um nó filho não toca no pai. */
    @Test
    public void nosDistintosGuardamPesosDistintos() {
        RecordingStore store = new RecordingStore();
        // 4-mosaico: raiz 'v' com duas pilhas; cada pilha é um nó 'h' com caminho próprio.
        LinearLayout root = buildLaidOut("4-mosaico", new GridLayoutEngine(ctx(), DIV_PX, MIN_PX), store);
        LinearLayout leftPile = (LinearLayout) root.getChildAt(0);
        assertEquals(3, leftPile.getChildCount()); // 2 células + 1 separadora

        drag(leftPile.getChildAt(1), false, 50f);

        assertTrue("nó filho não gravado", store.saved.containsKey("4-mosaico/r.0"));
        assertFalse("nó raiz gravado sem ser arrastado", store.saved.containsKey("4-mosaico/r"));
        assertEquals(2, store.saved.get("4-mosaico/r.0").length);
        assertSomaUm(store.saved.get("4-mosaico/r.0"));
    }
}
