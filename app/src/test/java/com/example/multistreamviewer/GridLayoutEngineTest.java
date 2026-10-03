package com.example.multistreamviewer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * Testes JVM da matemática do arraste e do catálogo de presets. O índice errado
 * duma separadora passou despercebido porque só corrompia a vista no aparelho;
 * estas invariantes são justamente as que esse bug violava.
 */
public class GridLayoutEngineTest {

    private static final float EPS = 1e-6f;

    private static float sum(float[] w) {
        float s = 0;
        for (float f : w) s += f;
        return s;
    }

    // ── índices no LinearLayout do nó ─────────────────────────────────────────

    /** walk() adiciona separadora e depois célula: a célula i está em 2*i, nunca em 2*i-1. */
    @Test
    public void celulasFicamNosIndicesPares() {
        for (int k = 2; k <= 6; k++) {
            int childCount = 2 * k - 1; // k células + (k-1) separadoras
            assertEquals(k, GridLayoutEngine.cellCount(childCount));
            for (int i = 0; i < k; i++) {
                assertEquals(2 * i, GridLayoutEngine.cellIndex(i));
                assertEquals("célula nunca numa separadora", 0, GridLayoutEngine.cellIndex(i) % 2);
            }
        }
    }

    /** A gravação tem de ter exactamente k entradas: com k+1, WeightsStore.get rejeita. */
    @Test
    public void pesosPersistidosTemTantasEntradasComoCelulas() {
        for (int k = 2; k <= 6; k++) {
            float[] w = new float[k];
            for (int i = 0; i < k; i++) w[i] = 1f / k;
            assertEquals(k, GridLayoutEngine.normalize(w).length);
        }
    }

    // ── dragWeights ───────────────────────────────────────────────────────────

    @Test
    public void arrasteConservaASomaDosDoisPesos() {
        float[] after = GridLayoutEngine.dragWeights(new float[]{0.5f, 0.5f}, 1, 0.2f, 0.1f);
        assertEquals(1f, sum(after), EPS);
        assertEquals(0.7f, after[0], EPS);
        assertEquals(0.3f, after[1], EPS);
    }

    @Test
    public void arrasteSoAlteraAsCelulasAdjacentes() {
        float[] after = GridLayoutEngine.dragWeights(
                new float[]{0.25f, 0.25f, 0.25f, 0.25f}, 2, 0.1f, 0.05f);
        assertEquals(0.25f, after[0], EPS);
        assertEquals(0.35f, after[1], EPS);
        assertEquals(0.15f, after[2], EPS);
        assertEquals(0.25f, after[3], EPS);
    }

    @Test
    public void arrasteLimiteEClampadoPeloMinimo() {
        assertArrayEquals(new float[]{0.9f, 0.1f},
                GridLayoutEngine.dragWeights(new float[]{0.5f, 0.5f}, 1, 5f, 0.1f), EPS);
        assertArrayEquals(new float[]{0.1f, 0.9f},
                GridLayoutEngine.dragWeights(new float[]{0.5f, 0.5f}, 1, -5f, 0.1f), EPS);
    }

    /** Mínimo maior do que metade do disponível não pode esmagar a célula vizinha. */
    @Test
    public void minimoInexequivelDivideMeioAMeio() {
        assertArrayEquals(new float[]{0.3f, 0.3f},
                GridLayoutEngine.dragWeights(new float[]{0.4f, 0.2f}, 1, 1f, 0.5f), EPS);
    }

    @Test
    public void fronteiraForaDoNoNaoAlteraNada() {
        float[] start = {0.5f, 0.5f};
        assertArrayEquals(start, GridLayoutEngine.dragWeights(start, 0, 0.3f, 0.1f), EPS);
        assertArrayEquals(start, GridLayoutEngine.dragWeights(start, 2, 0.3f, 0.1f), EPS);
        assertEquals(0.5f, start[0], EPS); // o array de entrada não é mutado
    }

    @Test
    public void pesosZerosNaoDividemPorZero() {
        assertArrayEquals(new float[]{0f, 0f},
                GridLayoutEngine.dragWeights(new float[]{0f, 0f}, 1, 0.3f, 0.1f), EPS);
    }

    // ── normalize ─────────────────────────────────────────────────────────────

    @Test
    public void normalizaSomaUm() {
        float[] w = GridLayoutEngine.normalize(new float[]{2f, 3f, 5f});
        assertEquals(1f, sum(w), EPS);
        assertArrayEquals(new float[]{0.2f, 0.3f, 0.5f}, w, EPS);
    }

    @Test
    public void pesoNegativoNaoRoubaEspaco() {
        assertArrayEquals(new float[]{0f, 0.5f, 0.5f},
                GridLayoutEngine.normalize(new float[]{-1f, 1f, 1f}), EPS);
    }

    @Test
    public void semEspacoParaRepartirNaoHaGravacao() {
        assertNull(GridLayoutEngine.normalize(new float[]{0f, 0f}));
    }

    /** Um arraste completo (todos os limites duma divisão) produz pesos aceite por get(…, k). */
    @Test
    public void arrasteAteAoFimProduzPesosValidos() {
        for (int k = 2; k <= 6; k++) {
            float[] w = new float[k];
            for (int i = 0; i < k; i++) w[i] = 1f / k;
            for (int b = 1; b < k; b++) {
                w = GridLayoutEngine.normalize(GridLayoutEngine.dragWeights(w, b, 0.37f, 0.02f));
                assertEquals(k, w.length);
            }
            assertEquals(1f, sum(w), 1e-5f);
        }
    }

    // ── catálogo de presets ───────────────────────────────────────────────────

    @Test
    public void todoPresetTemFolhasCoincidentesComN() {
        for (int n = 1; n <= 6; n++) {
            List<GridLayoutEngine.Preset> ps = GridLayoutEngine.presetsFor(n);
            assertFalse("sem presets para " + n + " boxes", ps.isEmpty());
            for (GridLayoutEngine.Preset p : ps) {
                assertEquals(n, p.n);
                assertEquals(p.id, n, GridLayoutEngine.countLeaves(p.tree));
            }
        }
    }

    /** Um nó com um só filho seria uma divisão sem separadora: não pode existir. */
    @Test
    public void nosIntermediariosTemPeloMenosDoisFilhos() {
        for (int n = 1; n <= 6; n++) {
            for (GridLayoutEngine.Preset p : GridLayoutEngine.presetsFor(n)) {
                checkAritidade(p.id, p.tree);
            }
        }
    }

    private void checkAritidade(String presetId, GridLayoutEngine.Node node) {
        if (node.leaf >= 0) return;
        assertTrue(presetId + ": nó com " + node.kids.size() + " filho(s)", node.kids.size() >= 2);
        assertTrue(presetId + ": direcção inválida", node.dir == 'v' || node.dir == 'h');
        for (GridLayoutEngine.Node k : node.kids) checkAritidade(presetId, k);
    }

    @Test
    public void byIdDesconhecidoDevolveNull() {
        assertEquals("4-mosaico", GridLayoutEngine.byId("4-mosaico").id);
        assertNull(GridLayoutEngine.byId("inexistente"));
        assertNull(GridLayoutEngine.byId(null));
    }

    /** build() cai no preset por omissão quando a contagem não bate; esse preset tem de existir. */
    @Test
    public void presetPorOMissaoResolveParaTodasContagens() {
        for (int n = 1; n <= 6; n++) {
            for (boolean portrait : new boolean[]{false, true}) {
                String id = GridLayoutEngine.defaultPresetId(n, portrait);
                GridLayoutEngine.Preset p = GridLayoutEngine.byId(id);
                assertNotNull(n + " (portrait=" + portrait + ") -> " + id, p);
                assertEquals(n, p.n);
            }
        }
    }

    /** O cap de boxes do modo TV (4) e o máx. do telemóvel (6) têm de ter presets. */
    @Test
    public void presetsExistemParaOCapDeBoxes() {
        for (int n : new int[]{4, 6}) {
            assertFalse(presetsFor(n + 1).isEmpty());
            assertTrue(presetsFor(n).size() >= 2);
        }
    }

    private List<GridLayoutEngine.Preset> presetsFor(int n) {
        return GridLayoutEngine.presetsFor(n);
    }
}
