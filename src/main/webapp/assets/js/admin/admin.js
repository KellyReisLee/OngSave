/* OngSave · base do painel do administrador: usuários, risco, ocorrências, finanças, menu, modal e notificações.
   Dados vindos do servidor (OS_ESTADO); as decisões vão para /api/admin/* e ficam na auditoria do banco. */
const $ = id => document.getElementById(id);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const fmt = (n, d = 0) => n.toLocaleString('pt-BR', { minimumFractionDigits: d, maximumFractionDigits: d });
const brl = v => (v < 0 ? '-R$ ' : 'R$ ') + fmt(Math.abs(v), Number.isInteger(v) ? 0 : 2);
const reduz = matchMedia('(prefers-reduced-motion: reduce)').matches;
const ADM = { nome: (OS.usuario && OS.usuario.nome) || document.body.dataset.usuario || 'Administrador', papel: 'Administrador' };
const quando = d => d === 0 ? 'Hoje' : new Date(Date.now() - d * 864e5).toLocaleDateString('pt-BR');
const data = ms => new Date(ms).toLocaleDateString('pt-BR');
const dataHora = ms => new Date(ms).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
const iniciais = n => (n || '?').split(' ').filter(w => w.length > 2).map(w => w[0]).slice(0, 2).join('').toUpperCase();
const csv = rows => '\ufeff' + rows.map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(';')).join('\n');
const baixar = (nome, txt, tipo) => { const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([txt], { type: tipo || 'text/plain;charset=utf-8' })); a.download = nome; a.click(); };
function aviso(msg) {
  const t = $('toast'); t.textContent = msg; t.classList.remove('hidden');
  clearTimeout(aviso.t); aviso.t = setTimeout(() => t.classList.add('hidden'), 4500);
}
function contar(el, alvo, f) {
  el.classList.remove('skel');
  if (reduz) { el.textContent = f(alvo); return; }
  const ini = performance.now();
  (function passo(t) {
    const k = Math.min((t - ini) / 700, 1);
    el.textContent = f(alvo * (1 - Math.pow(1 - k, 3)));
    if (k < 1) requestAnimationFrame(passo);
  })(ini);
}

/* ---------- Rótulos ---------- */
const TIPOS = { empresa: ['Empresa doadora', 'fa-building', 'bg-orange-50 text-orange-700'], motorista: ['Motorista', 'fa-truck', 'bg-emerald-50 text-emerald-700'], ong: ['ONG', 'fa-hands-holding-child', 'bg-blue-50 text-blue-700'] };
const PLURAL = { empresa: 'Empresas', motorista: 'Motoristas', ong: 'ONGs' };
const ST = {
  pendente: ['Pendente', 'bg-amber-50 text-amber-700 border-amber-200', 'fa-hourglass-half'],
  ativo: ['Ativo', 'bg-emerald-50 text-emerald-700 border-emerald-200', 'fa-circle-check'],
  suspenso: ['Suspenso', 'bg-orange-50 text-orange-700 border-orange-200', 'fa-pause'],
  bloqueado: ['Bloqueado', 'bg-red-50 text-red-700 border-red-200', 'fa-ban'],
  rejeitado: ['Rejeitado', 'bg-slate-100 text-slate-600 border-slate-200', 'fa-xmark']
};
const RISCO_COR = { alto: 'bg-red-50 text-red-700 border-red-200', medio: 'bg-amber-50 text-amber-700 border-amber-200', baixo: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
const pill = r => '<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ' + r[1] + '"><i class="fa-solid ' + r[2] + ' text-[10px]"></i> ' + r[0] + '</span>';
const pillRisco = n => pill([n === 'alto' ? 'Risco alto' : n === 'medio' ? 'Risco médio' : 'Risco baixo', RISCO_COR[n], n === 'alto' ? 'fa-triangle-exclamation' : n === 'medio' ? 'fa-circle-exclamation' : 'fa-shield-check']);
const tipoTag = t => '<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold ' + TIPOS[t][2] + '"><i class="fa-solid ' + TIPOS[t][1] + ' text-[10px]"></i> ' + TIPOS[t][0] + '</span>';
const DOCS = {
  empresa: ['Contrato social e CNPJ', 'Termo de responsabilidade técnica', 'Comprovante de endereço'],
  motorista: ['CNH', 'CRLV do veículo', 'Comprovante de residência', 'Foto do veículo'],
  ong: ['Alvará / registro da instituição', 'Estatuto e ata da diretoria', 'Comprovante de endereço', 'Documento do responsável']
};

/* ---------- Utilizadores (do servidor) ---------- */
const USUARIOS = OS_ESTADO.usuarios || [];
const porId = id => USUARIOS.find(u => u.id === +id);
const porNome = n => USUARIOS.find(u => u.nome === n);
const pendentes = () => USUARIOS.filter(u => u.status === 'pendente');

/* ---------- Risco: regras automáticas editáveis ---------- */
function risco(u) {
  const R = PLAT.regras, f = []; let n = 0;
  const add = (nv, txt) => { f.push([nv, txt]); n = Math.max(n, { info: 1, medio: 2, alto: 3 }[nv]); };
  if (u.st.entregas >= R.minEntregas && u.st.rej >= R.rejeicaoPct) add('alto', 'Rejeição de ' + u.st.rej + '% em ' + u.st.entregas + ' entregas (limite de ' + R.rejeicaoPct + '%)');
  else if (u.st.entregas >= R.minEntregas && u.st.rej >= R.rejeicaoPct / 2) add('medio', 'Rejeição de ' + u.st.rej + '%, acima da metade do limite');
  if (u.tipo === 'ong' && u.st.ocorr >= 3) add('medio', u.st.ocorr + ' ocorrências de recebimento nos últimos 30 dias');
  if (u.tipo === 'motorista' && u.st.ocorr >= 3) add('alto', u.st.ocorr + ' ocorrências registradas nos últimos 30 dias');
  const venc = u.tipo === 'motorista' ? u.cnhDias : u.tipo === 'ong' ? u.alvaraDias : null;
  if (venc !== null && venc !== undefined) { if (venc < 0) add('alto', 'Documento de habilitação ou registro vencido'); else if (venc < 30) add('medio', 'Documento vence em ' + venc + ' dias'); }
  u.docs.forEach(d => { if (d.status === 'rejeitado') add('alto', 'Documento rejeitado: ' + d.nome); else if (d.status === 'analise') add('info', 'Documento em análise: ' + d.nome); });
  PARES.filter(p => p.mot === u.nome || p.ong === u.nome).forEach(p => { if (p.pct >= R.concentracaoPct) add('medio', p.pct + '% das entregas entre ' + p.mot + ' e ' + p.ong + ' (limite de ' + R.concentracaoPct + '%)'); });
  return { nivel: ['baixo', 'baixo', 'medio', 'alto'][n], flags: f };
}
/* Concentração motorista x ONG nos últimos 90 dias: sinal clássico de conluio para liberar pagamento. */
const PARES = OS_ESTADO.pares || [];
const recomendacao = u => { const r = risco(u); return r.nivel === 'alto' && u.status === 'ativo' ? 'Suspender ou bloquear (regra automática)' : r.nivel === 'alto' && u.status === 'pendente' ? 'Rejeitar ou pedir novos documentos' : ''; };

/* ---------- Auditoria, ocorrências e ações ---------- */
const LOG = OS_ESTADO.log || [];
/* Registo local imediato (o servidor já gravou a auditoria; a próxima leitura traz a versão oficial). */
function logar(acao, alvo, extra, alvoId) { LOG.unshift({ t: Date.now(), ator: ADM.nome, acao, alvo, alvoId, extra: extra || '' }); }
const TXT_STATUS = { ativo: 'Aprovou o cadastro', suspenso: 'Suspendeu a conta', bloqueado: 'Bloqueou a conta', rejeitado: 'Rejeitou o cadastro', pendente: 'Reabriu a análise do cadastro' };
/* Muda a situação da conta no servidor (com justificativa) e atualiza a lista local. Lança erro com a mensagem do servidor. */
async function mudarStatus(u, novo, r) {
  await osApi('admin/usuarios/' + u.id + '/status', { status: novo, motivo: r ? r.motivo : '', obs: r ? r.obs : '' });
  const antes = u.status; u.status = novo;
  logar(novo === 'ativo' && antes !== 'pendente' ? 'Reativou a conta' : TXT_STATUS[novo], u.nome, r ? (r.motivo || '') + (r.obs ? (r.motivo ? ': ' : '') + r.obs : '') : '', u.id);
}
const OCORR = OS_ESTADO.ocorrencias || [];
const todasOcorr = () => OCORR;
const OC_ST = { aberta: ['Aberta', 'bg-red-50 text-red-700 border-red-200', 'fa-circle-exclamation'], analise: ['Em análise', 'bg-amber-50 text-amber-700 border-amber-200', 'fa-magnifying-glass'], resolvida: ['Resolvida', 'bg-emerald-50 text-emerald-700 border-emerald-200', 'fa-check'] };

/* ---------- Finanças (base para dashboard e financeiro) ---------- */
const empresasAtivas = () => USUARIOS.filter(u => u.tipo === 'empresa' && u.status === 'ativo' && PLAT.planos[u.plano]);
const mrr = () => empresasAtivas().reduce((s, u) => s + PLAT.planos[u.plano].preco, 0);
const extrasMes = () => empresasAtivas().reduce((s, u) => s + u.st.extras * PLAT.planos[u.plano].taxaExtra, 0);
const FIN = OS_ESTADO.fin || { receita: [0, 0, 0, 0, 0, 0], fretes: [0, 0, 0, 0, 0, 0], movimentos: [], faturas: [] };
const mesesFin = () => Array.from({ length: 6 }, (_, i) => new Date(new Date().getFullYear(), new Date().getMonth() - 5 + i, 1).toLocaleDateString('pt-BR', { month: 'short' }).replace('.', ''));
/* Últimos 6 meses de entradas (faturas); o mês corrente é a receita contratada (planos + lotes extra). */
const recSerie = () => FIN.receita;
const reservaSaldo = () => recSerie().reduce((s, v) => s + v, 0) * PLAT.reservaPct / 100;
const fretesMedia = () => FIN.fretes.slice(-3).reduce((s, v) => s + v, 0) / 3;
const cobertura = () => fretesMedia() > 0 ? Math.min(99, reservaSaldo() / fretesMedia()) : (reservaSaldo() > 0 ? 99 : 0);
const coberturaSt = c => c >= 3 ? ['Meta atingida', 'bg-emerald-50 text-emerald-700 border-emerald-200', 'fa-shield-check'] : c >= 2 ? ['Atenção', 'bg-amber-50 text-amber-700 border-amber-200', 'fa-circle-exclamation'] : ['Crítico', 'bg-red-50 text-red-700 border-red-200', 'fa-triangle-exclamation'];

/* ---------- Modal de motivo (ações sensíveis sempre pedem justificativa) ---------- */
function pedirMotivo(o) {
  return new Promise(res => {
    const v = document.createElement('div');
    v.className = 'fixed inset-0 z-[70] grid place-items-center p-4 bg-slate-900/50';
    v.innerHTML = '<div role="dialog" aria-modal="true" aria-labelledby="mm-t" class="bg-white rounded-3xl w-full max-w-md p-6 space-y-4"><h3 id="mm-t" class="text-lg font-extrabold text-slate-900">' + esc(o.titulo) + '</h3>' +
      '<p class="text-sm text-slate-500">' + esc(o.msg || '') + '</p>' +
      (o.motivos ? '<div><label class="rotulo" for="mm-m">Motivo</label><select id="mm-m" class="campo">' + o.motivos.map(m => '<option>' + esc(m) + '</option>').join('') + '</select></div>' : '') +
      '<div><label class="rotulo" for="mm-o">Observação' + (o.obrigatorio ? ' (obrigatória)' : '') + '</label><textarea id="mm-o" rows="3" maxlength="300" class="campo"></textarea><p class="msg-erro" id="mm-e"></p></div>' +
      '<div class="flex justify-end gap-3"><button id="mm-c" class="btn-sec">Cancelar</button><button id="mm-ok" class="btn-lar ' + (o.perigo ? '!bg-red-600 hover:!bg-red-700' : '') + '">' + esc(o.botao || 'Confirmar') + '</button></div></div>';
    document.body.appendChild(v);
    const tecla = e => { if (e.key === 'Escape') fim(null); };
    const fim = r => { v.remove(); document.removeEventListener('keydown', tecla); res(r); };
    document.addEventListener('keydown', tecla);
    v.querySelector('#mm-c').onclick = () => fim(null);
    v.querySelector('#mm-ok').onclick = () => {
      const obs = v.querySelector('#mm-o').value.trim();
      if (o.obrigatorio && !obs) { v.querySelector('#mm-e').textContent = 'Descreva o motivo.'; return; }
      fim({ motivo: o.motivos ? v.querySelector('#mm-m').value : '', obs });
    };
    (o.motivos ? v.querySelector('#mm-m') : v.querySelector('#mm-o')).focus();
  });
}
/* Fluxos de ação reutilizados por todas as páginas */
const MOTIVOS = { suspenso: ['Inadimplência', 'Documento vencido', 'Taxa de rejeição acima do limite', 'Investigação em andamento', 'Outro'], bloqueado: ['Fraude comprovada', 'Comportamento anômalo reiterado', 'Risco sanitário', 'Documentos falsos', 'Outro'], rejeitado: ['Documentação incompleta', 'Documento inválido', 'Fora da região de operação', 'Outro'] };
async function acao(u, novo) {
  if (novo === 'ativo' || novo === 'pendente') {
    try { await mudarStatus(u, novo); aviso(u.nome + ': ' + (novo === 'ativo' ? 'conta ativa.' : 'cadastro de volta à análise.')); return true; }
    catch (e) { aviso(e.message); return false; }
  }
  const r = await pedirMotivo({ titulo: { suspenso: 'Suspender conta', bloqueado: 'Bloquear conta', rejeitado: 'Rejeitar cadastro' }[novo] + ' de ' + u.nome, msg: 'A justificativa fica registrada na auditoria e pode ser contestada pelo usuário.', motivos: MOTIVOS[novo], obrigatorio: true, botao: 'Confirmar', perigo: novo !== 'suspenso' });
  if (!r) return false;
  try { await mudarStatus(u, novo, r); aviso(u.nome + ': ' + ST[novo][0].toLowerCase() + '.'); return true; }
  catch (e) { aviso(e.message); return false; }
}

/* ---------- Notificações ---------- */
const NOTIFS = OS_ESTADO.notifs || [];

/* ---------- Menu e topo ---------- */
(function montarShell() {
  const b = document.body.dataset;
  const link = (href, ic, txt, id) => '<a href="' + href + '" class="flex items-center gap-3 px-4 py-3 rounded-xl font-medium text-sm transition-all ' +
    (id === b.pagina ? 'bg-violet-600 text-white shadow-md shadow-violet-600/20' : 'text-slate-400 hover:text-white hover:bg-slate-800/60') + '"' + (id === b.pagina ? ' aria-current="page"' : '') + '><i class="fa-solid ' + ic + ' w-5"></i> ' + txt + '</a>';
  document.body.insertAdjacentHTML('afterbegin',
    '<div id="veu-menu" class="hidden fixed inset-0 bg-slate-900/50 z-30 md:hidden"></div>' +
    '<aside id="menu" class="w-64 bg-slate-900 text-slate-300 hidden md:flex flex-col justify-between border-r border-slate-800 fixed inset-y-0 left-0 z-40 md:sticky md:top-0 md:z-auto h-screen">' +
      '<div><div class="p-6 flex items-center gap-3 border-b border-slate-800/80"><div class="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-violet-600 flex items-center justify-center text-white shadow-lg shadow-violet-500/20"><i class="fa-solid fa-leaf text-sm"></i></div>' +
        '<div><span class="text-lg font-extrabold tracking-tight text-white">Ong<span class="text-violet-400">Save</span></span><span class="block text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Painel Administrador</span></div></div>' +
      '<nav class="p-4 space-y-1.5">' + link('dashboard', 'fa-gauge-high', 'Visão Geral', 'dashboard') + link('gerir-utilizadores', 'fa-users-gear', 'Utilizadores', 'usuarios') + link('financeiro', 'fa-scale-balanced', 'Financeiro', 'financeiro') + '</nav></div>' +
      '<div class="p-4 border-t border-slate-800"><a href="../logout" class="flex items-center gap-3 px-4 py-3 rounded-xl text-red-400 hover:bg-red-500/10 font-medium text-sm transition-all"><i class="fa-solid fa-right-from-bracket w-5"></i> Encerrar Sessão</a></div>' +
    '</aside>' +
    '<main class="flex-1 flex flex-col min-w-0">' +
      '<header class="bg-white border-b border-slate-200 h-20 px-4 md:px-8 flex items-center gap-3 sticky top-0 z-20">' +
        '<button id="btn-menu" class="md:hidden w-10 h-10 rounded-xl bg-slate-100 text-slate-600" aria-label="Abrir menu"><i class="fa-solid fa-bars"></i></button>' +
        '<h1 class="text-lg md:text-xl font-bold text-slate-900 tracking-tight">' + esc(b.titulo) + '</h1>' +
        '<span class="hidden xl:inline-block py-1 px-3 rounded-full bg-violet-50 text-violet-700 text-xs font-bold border border-violet-200"><i class="fa-solid fa-user-shield mr-1.5"></i>Administrador</span>' +
        '<div class="ml-auto flex items-center gap-3"><div id="topo-extra"></div>' +
          '<div class="relative"><button id="btn-notif" class="relative w-10 h-10 rounded-xl bg-slate-100 text-slate-600 hover:bg-violet-50 hover:text-violet-600 transition-colors" aria-label="Notificações" aria-expanded="false"><i class="fa-solid fa-bell"></i>' +
            '<span id="notif-n" class="hidden absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-violet-600 text-white text-[10px] font-bold grid place-items-center"></span></button>' +
            '<div id="painel-notif" class="hidden absolute right-0 mt-2 w-80 max-w-[85vw] bg-white border border-slate-200 rounded-2xl shadow-xl overflow-hidden"><p class="px-4 py-3 text-xs font-bold uppercase tracking-wider text-slate-400 border-b border-slate-100">Notificações</p>' +
            '<ul id="lista-notif" class="max-h-80 overflow-y-auto divide-y divide-slate-100"></ul></div></div>' +
          '<div class="text-right hidden sm:block"><p class="text-sm font-bold text-slate-800">' + esc(ADM.nome) + '</p><p class="text-xs text-slate-500">' + esc(ADM.papel) + '</p></div>' +
          '<div class="w-10 h-10 rounded-full bg-violet-100 text-violet-600 flex items-center justify-center font-bold border-2 border-violet-200">' + iniciais(ADM.nome) + '</div></div></header>' +
      '<div id="area" class="p-4 md:p-8 lg:p-10 space-y-6 max-w-7xl w-full mx-auto"></div>' +
      '<footer class="mt-auto py-6 px-10 text-center text-xs text-slate-400 border-t border-slate-200 bg-white"><p>&copy; 2026 OngSave Brasil. Plataforma de Logística Reversa B2B e Impacto ESG.</p></footer></main>');
  document.body.insertAdjacentHTML('beforeend', '<div id="toast" class="hidden fixed bottom-5 right-5 z-[80] max-w-sm bg-slate-900 text-white text-sm font-medium px-4 py-3 rounded-2xl shadow-xl" role="status"></div>');
  $('area').appendChild($('pagina'));
})();
function pintarNotifs() {
  const n = NOTIFS.filter(x => !x.lida).length;
  $('notif-n').textContent = n; $('notif-n').classList.toggle('hidden', n === 0);
  $('lista-notif').innerHTML = NOTIFS.map(x => '<li class="flex gap-3 px-4 py-3 text-sm ' + (x.lida ? '' : 'bg-violet-50/60') + '"><i class="fa-solid ' + x.ic + ' text-violet-500 mt-0.5"></i><div><p class="text-slate-700">' + esc(x.t) + '</p><p class="text-[11px] text-slate-400 mt-0.5">' + x.q + '</p></div></li>').join('');
}
function notificar(t, ic) { NOTIFS.unshift({ t, ic: ic || 'fa-bell', lida: false, q: 'agora' }); pintarNotifs(); aviso(t); }
$('btn-notif').onclick = e => {
  e.stopPropagation();
  const abrir = $('painel-notif').classList.toggle('hidden') === false;
  $('btn-notif').setAttribute('aria-expanded', abrir);
  if (abrir && NOTIFS.some(x => !x.lida)) { NOTIFS.forEach(x => { x.lida = true; }); osMarcarLidas(); setTimeout(pintarNotifs, 1200); }
};
document.addEventListener('click', e => { if (!$('painel-notif').contains(e.target)) $('painel-notif').classList.add('hidden'); });
const alternarMenu = () => { $('menu').classList.toggle('hidden'); $('menu').classList.toggle('flex'); $('veu-menu').classList.toggle('hidden'); };
$('btn-menu').onclick = alternarMenu; $('veu-menu').onclick = alternarMenu;
pintarNotifs();

/* ---------- Atualização ao vivo ---------- */
let aoMudarAdmin = null;
const assinaturaAdm = () => JSON.stringify([USUARIOS.map(u => [u.id, u.status, u.docs.map(d => d.status)]), OCORR.map(o => [o.id, o.status]), (OS_ESTADO.ops || []).map(o => [o.id, o.fase])]);
const atualizarDados = osAcompanhar('admin', e => {
  const antes = assinaturaAdm(), naoLidas = NOTIFS.filter(x => !x.lida).length;
  osSubstituir(USUARIOS, e.usuarios); osSubstituir(OCORR, e.ocorrencias); osSubstituir(LOG, e.log); osSubstituir(PARES, e.pares);
  osSubstituir(NOTIFS, e.notifs); osAtribuir(FIN, e.fin);
  if (OS_ESTADO.ops) osSubstituir(OS_ESTADO.ops, e.ops);
  if (OS_ESTADO.feed) osSubstituir(OS_ESTADO.feed, e.feed);
  if (OS_ESTADO.entregasSemana) osSubstituir(OS_ESTADO.entregasSemana, e.entregasSemana);
  pintarNotifs();
  if (NOTIFS.filter(x => !x.lida).length > naoLidas) aviso(NOTIFS[0].t);
  if (aoMudarAdmin) aoMudarAdmin(antes !== assinaturaAdm());
}, 15000);
