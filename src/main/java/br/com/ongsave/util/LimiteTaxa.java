package br.com.ongsave.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de pedidos por chave (IP, utilizador...) numa janela deslizante, em memória.
 *
 * <p>Protege contra força bruta e abuso (login, cadastro, recuperação de senha, API). Complementa o bloqueio
 * por conta (5 senhas erradas = 15 min), que sozinho deixaria um atacante testar muitas contas a partir do mesmo IP.
 * Numa instalação com várias instâncias, cada uma conta à parte (aceitável para este porte; em escala usar Redis).</p>
 */
public final class LimiteTaxa {

    private final int maximo;
    private final long janelaMs;
    private final Map<String, Deque<Long>> pedidos = new ConcurrentHashMap<>();
    private volatile long ultimaLimpeza = System.currentTimeMillis();

    public LimiteTaxa(int maximo, long janelaMs) {
        this.maximo = maximo;
        this.janelaMs = janelaMs;
    }

    /** Regista o pedido e devolve true se ainda está dentro do limite. */
    public boolean permitir(String chave) {
        long agora = System.currentTimeMillis();
        limparDeVezEmQuando(agora);
        Deque<Long> fila = pedidos.computeIfAbsent(chave == null ? "?" : chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() > janelaMs) fila.pollFirst();
            if (fila.size() >= maximo) return false;
            fila.addLast(agora);
            return true;
        }
    }

    /** Segundos até a chave voltar a ter vaga (para a mensagem ao utilizador). */
    public long segundosParaLiberar(String chave) {
        Deque<Long> fila = pedidos.get(chave == null ? "?" : chave);
        if (fila == null) return 0;
        synchronized (fila) {
            if (fila.isEmpty()) return 0;
            return Math.max(1, (fila.peekFirst() + janelaMs - System.currentTimeMillis() + 999) / 1000);
        }
    }

    private void limparDeVezEmQuando(long agora) {
        if (agora - ultimaLimpeza < 60_000) return;
        ultimaLimpeza = agora;
        pedidos.entrySet().removeIf(e -> {
            Deque<Long> f = e.getValue();
            synchronized (f) { return f.isEmpty() || agora - f.peekLast() > janelaMs; }
        });
    }
}
