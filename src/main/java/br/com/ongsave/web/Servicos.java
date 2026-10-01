package br.com.ongsave.web;

import br.com.ongsave.config.Configuracao;
import br.com.ongsave.service.AdminService;
import br.com.ongsave.service.AuditoriaService;
import br.com.ongsave.service.AutenticacaoService;
import br.com.ongsave.service.EmpresaService;
import br.com.ongsave.service.Geocodificador;
import br.com.ongsave.service.MotoristaService;
import br.com.ongsave.service.NotificacaoService;
import br.com.ongsave.service.OngService;
import br.com.ongsave.service.PlataformaService;
import br.com.ongsave.service.Simulador;
import jakarta.servlet.ServletContext;

/** Todos os serviços da aplicação, criados uma única vez no arranque e partilhados pelo ServletContext. */
public final class Servicos {

    public static final String ATRIBUTO = "ongsave.servicos";

    public final Configuracao config;
    public final AuditoriaService auditoria = new AuditoriaService();
    public final NotificacaoService notificacoes = new NotificacaoService();
    public final PlataformaService plataforma;
    public final AutenticacaoService autenticacao;
    public final EmpresaService empresa;
    public final MotoristaService motorista;
    public final OngService ong;
    public final AdminService admin;
    /** {@code null} quando app.simulacao=false. */
    public final Simulador simulador;

    public Servicos(Configuracao config) {
        this.config = config;
        Geocodificador geo = new Geocodificador(config.geocodificar());
        plataforma = new PlataformaService(auditoria);
        autenticacao = new AutenticacaoService(notificacoes, auditoria);
        empresa = new EmpresaService(plataforma, notificacoes, auditoria, geo);
        motorista = new MotoristaService(plataforma, notificacoes, auditoria, config.geoValidar(), config.raioValidacaoMetros(),
                config.simulacao());
        ong = new OngService(plataforma, notificacoes, auditoria, geo, config.geoValidar(), config.simulacao());
        admin = new AdminService(plataforma, notificacoes, auditoria);
        simulador = config.simulacao()
                ? new Simulador(empresa, motorista, ong, config.simSegundosTrecho(), config.simFrota(), config.simContasManuais())
                : null;
    }

    /** {@code null} quando a aplicação arrancou sem banco (ver {@link Inicializador#ERRO}). */
    public static Servicos de(ServletContext ctx) {
        return (Servicos) ctx.getAttribute(ATRIBUTO);
    }
}
