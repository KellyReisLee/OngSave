/* OngSave · base compartilhada das páginas da ONG: perfil, utilitários, menu, topo e notificações.
   Os dados vêm do servidor (OS_ESTADO, injetado pela JSP) e as ações vão para /api/ong/*. */
const $ = id => document.getElementById(id);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const fmt = (n, d = 0) => n.toLocaleString('pt-BR', { minimumFractionDigits: d, maximumFractionDigits: d });
const reduz = matchMedia('(prefers-reduced-motion: reduce)').matches;
const REFEICOES_POR_KG = PLAT.metodologia.refeicoesPorKg; // definido pelo administrador (plataforma.js)

/* Categorias que a plataforma reconhece (as marcadas no perfil limitam as propostas aceites). */
const PADRAO = {
  categorias: ['Laticínios e Frios', 'Hortifrúti / Frutas e Verduras', 'Padaria e Panificados', 'Mercearia / Não Perecíveis', 'Pratos Prontos / Marmitas']
};
const ONG_DADOS = OS_ESTADO.ong || { nome: '', fantasia: '', cnpj: '', categorias: [], familias: 0, capKg: 0, horario: '', end: '', alvaraDias: 0, lat: -23.5505, lon: -46.6333 };
const perfilOng = () => ONG_DADOS;

const quando = d => d === 0 ? 'Hoje' : new Date(Date.now() - d * 864e5).toLocaleDateString('pt-BR');
const hm = h => h < 1 ? Math.round(h * 60) + ' min' : Math.floor(h) + 'h' + String(Math.round((h % 1) * 60)).padStart(2, '0');
function km(a, b) {
  const r = Math.PI / 180, dLa = (b[0] - a[0]) * r, dLo = (b[1] - a[1]) * r;
  const x = Math.sin(dLa / 2) ** 2 + Math.cos(a[0] * r) * Math.cos(b[0] * r) * Math.sin(dLo / 2) ** 2;
  return 12742 * Math.asin(Math.sqrt(x));
}
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

const NOTIFS = OS_ESTADO.notifs || [];

/* ---------- Menu lateral e topo ---------- */
(function montarShell() {
  const b = document.body.dataset, e = perfilOng();
  const link = (href, ic, txt, id) => '<a href="' + href + '" class="flex items-center gap-3 px-4 py-3 rounded-xl font-medium text-sm transition-all ' +
    (id === b.pagina ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20' : 'text-slate-400 hover:text-white hover:bg-slate-800/60') +
    '"' + (id === b.pagina ? ' aria-current="page"' : '') + '><i class="fa-solid ' + ic + ' w-5"></i> ' + txt + '</a>';
  document.body.insertAdjacentHTML('afterbegin',
    '<div id="veu-menu" class="hidden fixed inset-0 bg-slate-900/50 z-30 md:hidden"></div>' +
    '<aside id="menu" class="w-64 bg-slate-900 text-slate-300 hidden md:flex flex-col justify-between border-r border-slate-800 fixed inset-y-0 left-0 z-40 md:sticky md:top-0 md:z-auto h-screen">' +
      '<div><div class="p-6 flex items-center gap-3 border-b border-slate-800/80">' +
        '<div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/20"><i class="fa-solid fa-leaf text-sm"></i></div>' +
        '<div><span class="text-lg font-extrabold tracking-tight text-white">Ong<span class="text-blue-400">Save</span></span>' +
        '<span class="block text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Painel ONG</span></div></div>' +
      '<nav class="p-4 space-y-1.5">' +
        link('dashboard', 'fa-chart-pie', 'Visão Geral', 'dashboard') +
        link('confirmar-entrega', 'fa-key', 'Confirmar Entrega', 'confirmar-entrega') +
        link('perfil', 'fa-building-ngo', 'Perfil da Instituição', 'perfil') +
      '</nav></div>' +
      '<div class="p-4 border-t border-slate-800"><a href="../logout" class="flex items-center gap-3 px-4 py-3 rounded-xl text-red-400 hover:bg-red-500/10 font-medium text-sm transition-all"><i class="fa-solid fa-right-from-bracket w-5"></i> Encerrar Sessão</a></div>' +
    '</aside>' +
    '<main class="flex-1 flex flex-col min-w-0">' +
      '<header class="bg-white border-b border-slate-200 h-20 px-4 md:px-8 flex items-center gap-3 sticky top-0 z-20">' +
        '<button id="btn-menu" class="md:hidden w-10 h-10 rounded-xl bg-slate-100 text-slate-600" aria-label="Abrir menu"><i class="fa-solid fa-bars"></i></button>' +
        '<h1 class="text-lg md:text-xl font-bold text-slate-900 tracking-tight">' + esc(b.titulo) + '</h1>' +
        '<span class="hidden xl:inline-block py-1 px-3 rounded-full bg-emerald-50 text-emerald-700 text-xs font-bold border border-emerald-200"><i class="fa-solid fa-circle text-[8px] mr-1.5 animate-pulse text-emerald-500"></i>Cadastro aprovado</span>' +
        '<div class="ml-auto flex items-center gap-3"><div id="topo-extra"></div>' +
          '<div class="relative"><button id="btn-notif" class="relative w-10 h-10 rounded-xl bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-blue-600 transition-colors" aria-label="Notificações" aria-expanded="false"><i class="fa-solid fa-bell"></i>' +
            '<span id="notif-n" class="hidden absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-blue-600 text-white text-[10px] font-bold grid place-items-center"></span></button>' +
            '<div id="painel-notif" class="hidden absolute right-0 mt-2 w-80 max-w-[85vw] bg-white border border-slate-200 rounded-2xl shadow-xl overflow-hidden">' +
              '<p class="px-4 py-3 text-xs font-bold uppercase tracking-wider text-slate-400 border-b border-slate-100">Notificações</p>' +
              '<ul id="lista-notif" class="max-h-80 overflow-y-auto divide-y divide-slate-100"></ul></div></div>' +
          '<div class="text-right hidden sm:block"><p id="top-nome" class="text-sm font-bold text-slate-800">' + esc(e.fantasia) + '</p><p class="text-xs text-slate-500">CNPJ: ' + esc(e.cnpj) + '</p></div>' +
          '<div id="top-ini" class="w-10 h-10 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold border-2 border-blue-200">' + esc(e.fantasia.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()) + '</div>' +
        '</div></header>' +
      '<div id="area" class="p-4 md:p-8 lg:p-10 space-y-6 max-w-7xl w-full mx-auto"></div>' +
      '<footer class="mt-auto py-6 px-10 text-center text-xs text-slate-400 border-t border-slate-200 bg-white"><p>&copy; 2026 OngSave Brasil. Plataforma de Logística Reversa B2B e Impacto ESG.</p></footer>' +
    '</main>');
  document.body.insertAdjacentHTML('beforeend', '<div id="toast" class="hidden fixed bottom-5 right-5 z-[60] max-w-sm bg-slate-900 text-white text-sm font-medium px-4 py-3 rounded-2xl shadow-xl" role="status"></div>');
  $('area').appendChild($('pagina'));
})();

/* ---------- Notificações ---------- */
function pintarNotifs() {
  const n = NOTIFS.filter(x => !x.lida).length;
  $('notif-n').textContent = n; $('notif-n').classList.toggle('hidden', n === 0);
  $('lista-notif').innerHTML = NOTIFS.map(x =>
    '<li class="flex gap-3 px-4 py-3 text-sm ' + (x.lida ? '' : 'bg-blue-50/60') + '"><i class="fa-solid ' + x.ic + ' text-blue-500 mt-0.5"></i><div><p class="text-slate-700">' + esc(x.t) + '</p><p class="text-[11px] text-slate-400 mt-0.5">' + x.q + '</p></div></li>').join('');
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

/* ---------- Doações (vindas do servidor e atualizadas a cada poucos segundos) ---------- */
const ORG = [ONG_DADOS.lat, ONG_DADOS.lon];
const CONS = { amb: 'Temperatura ambiente', ref: 'Refrigerado (2 a 8 °C)', cong: 'Congelado' };
const DOACOES = OS_ESTADO.doacoes || [];
const DISP = OS_ESTADO.disp || [];
const ATIVO = { caminho: ['A caminho', 'bg-amber-50 text-amber-700 border-amber-200', 'fa-truck-fast'], porta: ['Motorista na porta', 'bg-blue-50 text-blue-700 border-blue-200', 'fa-door-open'] };
const SIT = {
  pendente: ['Aguardando conferência', 'bg-amber-50 text-amber-700 border-amber-200', 'fa-clipboard-question'],
  conferida: ['Conferida', 'bg-emerald-50 text-emerald-700 border-emerald-200', 'fa-clipboard-check'],
  ocorrencia: ['Com ocorrência', 'bg-red-50 text-red-700 border-red-200', 'fa-triangle-exclamation']
};
const PASSOS = ['Doação publicada', 'Motorista a caminho', 'Motorista na porta', 'Validada com código', 'Recebimento conferido'];
const pill = r => '<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ' + r[1] + '"><i class="fa-solid ' + r[2] + ' text-[10px]"></i> ' + r[0] + '</span>';
const ativas = () => DOACOES.filter(l => l.estado === 'caminho' || l.estado === 'porta');
const recebidas = () => DOACOES.filter(l => l.estado === 'recebida');
const situacao = l => !l.conf ? 'pendente' : l.conf.cond === 'ok' && !l.conf.retido ? 'conferida' : 'ocorrencia';
const origem = l => [l.empLat, l.empLon];
/* Posição do motorista: a última recebida do GPS; sem ela, estimada pelo progresso do trajeto. */
const pos = l => l.posLat != null ? [l.posLat, l.posLon] : (o => [o[0] + (ORG[0] - o[0]) * l.prog, o[1] + (ORG[1] - o[1]) * l.prog])(origem(l));
const eta = l => Math.max(1, Math.round(km(pos(l), ORG) / 25 * 60));
const baixar = (nome, txt, tipo) => { const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([txt], { type: tipo || 'text/plain;charset=utf-8' })); a.download = nome; a.click(); };

const tokenAtivo = l => l && l.token && !l.token.usado && l.token.expira > Date.now() ? l.token : null;
/* Antes de aceitar uma proposta, a ONG vê se consegue receber: categoria, câmara fria e capacidade do dia. */
const previstoHoje = () => DOACOES.filter(l => l.estado === 'caminho' || l.estado === 'porta' || (l.estado === 'recebida' && l.dias === 0)).reduce((s, l) => s + l.kg, 0) + DISP.filter(x => x.aceita).reduce((s, x) => s + x.kg, 0);
function incompativel(d) {
  const e = perfilOng();
  if (!e.categorias.includes(d.tipo)) return 'Categoria fora das que a instituição aceita';
  if (d.cons !== 'amb' && !e.camaraFria) return 'Exige câmara fria, que a instituição não tem';
  if (previstoHoje() + d.kg > e.capKg) return 'Passa da capacidade de hoje (' + fmt(e.capKg) + ' kg)';
  return '';
}
/* Conferência: diferença de peso acima da tolerância, ou alimento impróprio, abre ocorrência e retém o frete (regra no servidor). */
async function salvarConferencia(l, kg, cond, obs) {
  try {
    l.conf = await osApi('ong/lotes/' + l.id + '/conferencia', { kg, cond, obs });
    aviso(l.conf.retido ? 'Ocorrência do lote #' + l.id + ' enviada à administração. O frete do motorista fica retido até a análise.'
      : 'Recebimento do lote #' + l.id + ' conferido. Frete liberado ao motorista.');
    atualizarDados();
    return true;
  } catch (e) { aviso(e.message); return false; }
}
async function gerarToken(l) {
  try {
    l.token = await osApi('ong/lotes/' + l.id + '/token', {});
    aviso('Código gerado. Passe-o ao motorista.');
  } catch (e) { aviso(e.message); }
}

/* ---------- Atualização ao vivo ---------- */
let aoMudarPagina = null, aoTickPagina = null;
const assinatura = () => JSON.stringify([DOACOES.map(l => [l.id, l.estado, l.token && l.token.codigo, l.token && l.token.usado, !!l.conf]), DISP.map(d => [d.id, d.aceita])]);
function aplicarEstado(e) {
  const antes = assinatura();
  osSubstituir(DOACOES, e.doacoes); osSubstituir(DISP, e.disp);
  const naoLidas = NOTIFS.filter(x => !x.lida).length;
  osSubstituir(NOTIFS, e.notifs);
  if (e.ong) Object.assign(ONG_DADOS, e.ong);
  if (typeof aoEstado === 'function') aoEstado(e);
  if (NOTIFS.filter(x => !x.lida).length > naoLidas) { pintarNotifs(); aviso(NOTIFS[0].t); } else pintarNotifs();
  if (antes !== assinatura()) { if (aoMudarPagina) aoMudarPagina(); } else if (aoTickPagina) aoTickPagina();
}
const atualizarDados = osAcompanhar('ong', aplicarEstado, 8000);
/* Mantido o nome antigo: as páginas registam aqui o que redesenhar quando os dados mudam. */
function iniciarSimulacao(aoMudar, aoTick) { aoMudarPagina = aoMudar; aoTickPagina = aoTick || null; }

pintarNotifs();
