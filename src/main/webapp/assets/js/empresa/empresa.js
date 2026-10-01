/* OngSave · base compartilhada das páginas da empresa: estado vindo do servidor (OS_ESTADO), utilitários,
   menu, topo e notificações. As ações vão para /api/empresa/*; o estado é relido a cada poucos segundos. */
const $ = id => document.getElementById(id);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const fmt = (n, d = 0) => n.toLocaleString('pt-BR', { minimumFractionDigits: d, maximumFractionDigits: d });
const reduz = matchMedia('(prefers-reduced-motion: reduce)').matches;
const CO2_POR_KG = PLAT.metodologia.co2PorKg, REFEICOES_POR_KG = PLAT.metodologia.refeicoesPorKg; // definidos pelo administrador (plataforma.js)

/* localStorage só guarda o rascunho do formulário de novo lote (nada de dados da conta). */
const guardar = (k, v) => { try { localStorage.setItem('ongsave.emp.' + k, JSON.stringify(v)); } catch (e) { /* modo privado */ } };
const ler = (k, padrao) => { try { const v = JSON.parse(localStorage.getItem('ongsave.emp.' + k)); return v ?? padrao; } catch (e) { return padrao; } };

const PLANOS = PLAT.planos; // preço, franquia de lotes e taxa por lote extra vêm do administrador
const EMP = OS_ESTADO.empresa || { nome: '', cnpj: '', plano: 'media', lat: -23.5505, lon: -46.6333 };
const plano = () => PLANOS[EMP.plano] || PLANOS.media;
const perfilEmpresa = () => EMP;
function proxCobranca() {
  const d = new Date(); d.setMonth(d.getMonth() + 1); d.setDate(1);
  return d.toLocaleDateString('pt-BR');
}

const EMPRESA = [EMP.lat, EMP.lon];
const ONGS = OS_ESTADO.ongs || {};            // nome -> [lat, lon] das ONGs ativas
const LOTES = OS_ESTADO.lotes || [];
const horas = l => l.validade ? (new Date(l.validade) - Date.now()) / 36e5 : l.h;
const destino = l => l.ongLat != null ? [l.ongLat, l.ongLon] : (ONGS[l.ong] || EMPRESA);
let USO_MES = OS_ESTADO.usoMes || 0;
/* Franquia: lotes publicados no mês (não cancelados) contra o limite do plano. */
function usoPlano() {
  const p = plano(), uso = USO_MES, extras = Math.max(0, uso - p.franquia);
  return { uso, franquia: p.franquia, extras, taxa: p.taxaExtra, custoExtra: extras * p.taxaExtra };
}

const ESTADOS = {
      aguardando: ['Aguardando motorista', 'bg-slate-100 text-slate-600 border-slate-200', 'fa-hourglass-half'],
      aceito: ['Motorista a caminho', 'bg-blue-50 text-blue-700 border-blue-200', 'fa-user-clock'],
      transito: ['Em trânsito', 'bg-amber-50 text-amber-700 border-amber-200', 'fa-truck-fast'],
      entregue: ['Entregue com sucesso', 'bg-emerald-50 text-emerald-700 border-emerald-200', 'fa-check'],
      cancelado: ['Cancelado', 'bg-red-50 text-red-700 border-red-200', 'fa-ban']
    };
    const PASSOS = ['Lote publicado', 'Motorista aceitou', 'Carga coletada', 'Entregue e validado'];
    const FEITOS = { aguardando: 1, aceito: 2, transito: 3, entregue: 4, cancelado: 1 };

/* ---------- Utilitários ---------- */
const badge = e => '<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ' + ESTADOS[e][1] + '"><i class="fa-solid ' + ESTADOS[e][2] + ' text-[10px]"></i> ' + ESTADOS[e][0] + '</span>';
const quando = d => d === 0 ? 'Hoje' : new Date(Date.now() - d * 864e5).toLocaleDateString('pt-BR');
const hm = h => h < 1 ? Math.round(h * 60) + ' min' : Math.floor(h) + 'h' + String(Math.round((h % 1) * 60)).padStart(2, '0');
const ativo = l => l.estado === 'aceito' || l.estado === 'transito';
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
function timeline(l) {
  const n = FEITOS[l.estado];
  return '<ol class="space-y-3">' + PASSOS.map((p, i) => {
    const ok = i < n, atual = ok && i === n - 1 && (l.estado === 'aceito' || l.estado === 'transito');
    return '<li class="flex items-center gap-3 text-sm ' + (ok ? 'text-slate-800 font-semibold' : 'text-slate-400') + '">' +
      '<span class="w-6 h-6 rounded-full grid place-items-center text-[10px] ' + (ok ? 'bg-emerald-500 text-white' : 'bg-slate-100') + (atual ? ' ring-4 ring-emerald-100' : '') + '">' +
      '<i class="fa-solid ' + (ok ? 'fa-check' : 'fa-circle text-[5px]') + '"></i></span>' + p + '</li>';
  }).join('') + '</ol>' + (l.estado === 'cancelado' ? '<p class="mt-3 text-xs font-semibold text-red-600"><i class="fa-solid fa-ban"></i> Lote cancelado pela empresa.</p>' : '');
}


const NOTIFS = OS_ESTADO.notifs || [];

/* ---------- Menu lateral e topo ---------- */
(function montarShell() {
  const b = document.body.dataset, e = perfilEmpresa(), p = plano();
  const link = (href, ic, txt, id) => '<a href="' + href + '" class="flex items-center gap-3 px-4 py-3 rounded-xl font-medium text-sm transition-all ' +
    (id === b.pagina ? 'bg-orange-600 text-white shadow-md shadow-orange-600/20' : 'text-slate-400 hover:text-white hover:bg-slate-800/60') +
    '"' + (id === b.pagina ? ' aria-current="page"' : '') + '><i class="fa-solid ' + ic + ' w-5"></i> ' + txt + '</a>';
  document.body.insertAdjacentHTML('afterbegin',
    '<div id="veu-menu" class="hidden fixed inset-0 bg-slate-900/50 z-30 md:hidden"></div>' +
    '<aside id="menu" class="w-64 bg-slate-900 text-slate-300 hidden md:flex flex-col justify-between border-r border-slate-800 fixed inset-y-0 left-0 z-40 md:sticky md:top-0 md:z-auto h-screen">' +
      '<div><div class="p-6 flex items-center gap-3 border-b border-slate-800/80">' +
        '<div class="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center text-white shadow-lg shadow-orange-500/20"><i class="fa-solid fa-leaf text-sm"></i></div>' +
        '<div><span class="text-lg font-extrabold tracking-tight text-white">Ong<span class="text-orange-500">Save</span></span>' +
        '<span class="block text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Painel Empresa</span></div></div>' +
      '<nav class="p-4 space-y-1.5">' +
        link('dashboard', 'fa-chart-pie', 'Visão Geral', 'dashboard') +
        link('nova-doacao', 'fa-box-open', 'Registar Excedente', 'nova-doacao') +
        link('relatorio-esg', 'fa-chart-line', 'Relatórios ESG', 'relatorio-esg') +
        link('perfil', 'fa-building-shield', 'Subscrição &amp; Perfil', 'perfil') +
      '</nav></div>' +
      '<div class="p-4 border-t border-slate-800"><a href="../logout" class="flex items-center gap-3 px-4 py-3 rounded-xl text-red-400 hover:bg-red-500/10 font-medium text-sm transition-all"><i class="fa-solid fa-right-from-bracket w-5"></i> Encerrar Sessão</a></div>' +
    '</aside>' +
    '<main class="flex-1 flex flex-col min-w-0">' +
      '<header class="bg-white border-b border-slate-200 h-20 px-4 md:px-8 flex items-center gap-3 sticky top-0 z-20">' +
        '<button id="btn-menu" class="md:hidden w-10 h-10 rounded-xl bg-slate-100 text-slate-600" aria-label="Abrir menu"><i class="fa-solid fa-bars"></i></button>' +
        '<h1 class="text-lg md:text-xl font-bold text-slate-900 tracking-tight">' + esc(b.titulo) + '</h1>' +
        '<span class="hidden xl:inline-block py-1 px-3 rounded-full bg-emerald-50 text-emerald-700 text-xs font-bold border border-emerald-200"><i class="fa-solid fa-circle text-[8px] mr-1.5 animate-pulse text-emerald-500"></i><span id="pill-txt">Subscrição B2B Ativa (' + p.nome + ')</span></span>' +
        '<div class="ml-auto flex items-center gap-3"><div id="topo-extra"></div>' +
          '<div class="relative"><button id="btn-notif" class="relative w-10 h-10 rounded-xl bg-slate-100 text-slate-600 hover:bg-orange-50 hover:text-orange-600 transition-colors" aria-label="Notificações" aria-expanded="false"><i class="fa-solid fa-bell"></i>' +
            '<span id="notif-n" class="hidden absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-orange-600 text-white text-[10px] font-bold grid place-items-center"></span></button>' +
            '<div id="painel-notif" class="hidden absolute right-0 mt-2 w-80 max-w-[85vw] bg-white border border-slate-200 rounded-2xl shadow-xl overflow-hidden">' +
              '<p class="px-4 py-3 text-xs font-bold uppercase tracking-wider text-slate-400 border-b border-slate-100">Notificações</p>' +
              '<ul id="lista-notif" class="max-h-80 overflow-y-auto divide-y divide-slate-100"></ul></div></div>' +
          '<div class="text-right hidden sm:block"><p id="top-nome" class="text-sm font-bold text-slate-800">' + esc(e.nome) + '</p><p class="text-xs text-slate-500">CNPJ: <span id="top-cnpj">' + esc(e.cnpj) + '</span></p></div>' +
          '<div id="top-ini" class="w-10 h-10 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center font-bold border-2 border-orange-200">' + esc(e.nome.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()) + '</div>' +
        '</div></header>' +
      '<div id="area" class="p-4 md:p-8 lg:p-10 space-y-6 max-w-7xl w-full mx-auto"></div>' +
      '<footer class="mt-auto py-6 px-10 text-center text-xs text-slate-400 border-t border-slate-200 bg-white"><p>&copy; 2026 OngSave Brasil. Plataforma de Logística Reversa B2B e Impacto ESG.</p></footer>' +
    '</main>');
  document.body.insertAdjacentHTML('beforeend', '<div id="toast" class="hidden fixed bottom-5 right-5 z-[60] max-w-sm bg-slate-900 text-white text-sm font-medium px-4 py-3 rounded-2xl shadow-xl" role="status"></div>');
  $('area').appendChild($('pagina'));
})();

function pintarNotifs() {
  const n = NOTIFS.filter(x => !x.lida).length;
  $('notif-n').textContent = n; $('notif-n').classList.toggle('hidden', n === 0);
  $('lista-notif').innerHTML = NOTIFS.map(x =>
    '<li class="flex gap-3 px-4 py-3 text-sm ' + (x.lida ? '' : 'bg-orange-50/50') + '"><i class="fa-solid ' + x.ic + ' text-orange-500 mt-0.5"></i><div><p class="text-slate-700">' + esc(x.t) + '</p><p class="text-[11px] text-slate-400 mt-0.5">' + x.q + '</p></div></li>').join('');
}
function notificar(t, ic) { NOTIFS.unshift({ t, ic, lida: false, q: 'agora' }); pintarNotifs(); aviso(t); }
$('btn-notif').onclick = e => {
  e.stopPropagation();
  const abrir = $('painel-notif').classList.toggle('hidden') === false;
  $('btn-notif').setAttribute('aria-expanded', abrir);
  if (abrir && NOTIFS.some(x => !x.lida)) { NOTIFS.forEach(x => { x.lida = true; }); osMarcarLidas(); setTimeout(pintarNotifs, 1200); }
};
document.addEventListener('click', e => { if (!$('painel-notif').contains(e.target)) $('painel-notif').classList.add('hidden'); });


/* ---------- Menu no celular ---------- */
const menu = () => { $('menu').classList.toggle('hidden'); $('menu').classList.toggle('flex'); $('veu-menu').classList.toggle('hidden'); };
$('btn-menu').onclick = menu; $('veu-menu').onclick = menu;

/* ---------- Atualização ao vivo (estado relido do servidor) ---------- */
let aoMudarPagina = null, aoTickPagina = null;
const assinatura = () => JSON.stringify(LOTES.map(l => [l.id, l.estado, l.ongAceitou, !!l.retirada, !!l.conf, l.mot, l.posLat != null]));
function aplicarEstado(e) {
  const antes = assinatura(), naoLidas = NOTIFS.filter(x => !x.lida).length;
  osSubstituir(LOTES, e.lotes); osSubstituir(NOTIFS, e.notifs);
  if (e.empresa) Object.assign(EMP, e.empresa);
  if (e.ongs) osAtribuir(ONGS, e.ongs);
  USO_MES = e.usoMes || 0;
  if (typeof aoEstado === 'function') aoEstado(e);
  pintarNotifs();
  if (NOTIFS.filter(x => !x.lida).length > naoLidas) aviso(NOTIFS[0].t);
  if (antes !== assinatura()) { if (aoMudarPagina) aoMudarPagina(); } else if (aoTickPagina) aoTickPagina();
}
const atualizarDados = osAcompanhar('empresa', aplicarEstado, 10000);
function aoAtualizar(mudou, tick) { aoMudarPagina = mudou; aoTickPagina = tick || null; }

pintarNotifs();
