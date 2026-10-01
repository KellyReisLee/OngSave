package br.com.ongsave.model;

import java.util.Arrays;
import java.util.Optional;

/** Os quatro perfis da plataforma. O "segmento" é a pasta/URL de cada painel. */
public enum Perfil {
    EMPRESA("empresa", "Empresa doadora"),
    MOTORISTA("motorista", "Motorista parceiro"),
    ONG("ong", "ONG"),
    ADMIN("admin", "Administrador");

    private final String segmento;
    private final String descricao;

    Perfil(String segmento, String descricao) {
        this.segmento = segmento;
        this.descricao = descricao;
    }

    /** Usado na EL: ${usuarioLogado.perfil.segmento} */
    public String getSegmento() { return segmento; }
    public String getDescricao() { return descricao; }

    public static Optional<Perfil> doSegmento(String s) {
        return Arrays.stream(values()).filter(p -> p.segmento.equals(s)).findFirst();
    }
}
