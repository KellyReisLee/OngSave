/* Dashboard da ONG: chegadas, código de validação, propostas de doação, recebimentos e conferência.
   Dados do servidor (OS_ESTADO), atualizados a cada poucos segundos por ong.js. */

const PER = OS_ESTADO.per || {};
const PARCEIROS = OS_ESTADO.parceiros || [];
function aoEstado(e) { if (e.per) osAtribuir(PER, e.per); if (e.parceiros) osSubstituir(PARCEIROS, e.parceiros); }


/* ---------- Resumo, alertas e capacidade ---------- */
function resumo() {
  const c = DOACOES.filter(l => l.estado === 'caminho').length, p = DOACOES.filter(l => l.estado === 'porta').length;
  $('banner-tit').textContent = 'Olá, ' + perfilOng().fantasia + '!';
  $('banner-resumo').textContent = c + p === 0
    ? 'Nenhuma doação em andamento agora. Veja o que está disponível na região.'
    : c + ' a caminho' + (p ? ' e ' + p + ' com o motorista na porta, aguardando o seu código de validação.' : '.');
  $('k-ativos').textContent = (c + p) + ' a caminho';
}
const descartados = new Set();
function alertas() {
  const e = perfilOng(), a = [];
  DOACOES.filter(l => l.estado === 'porta').forEach(l => a.push(['p' + l.id, 'blue', 'fa-door-open', 'O motorista ' + l.mot + ' está na porta com o lote #' + l.id + '. Gere o código de validação.']));
  ativas().filter(l => l.cons !== 'amb').forEach(l => a.push(['f' + l.id, 'sky', 'fa-snowflake', 'O lote #' + l.id + ' é ' + CONS[l.cons].toLowerCase() + ' e chega em ' + (l.estado === 'porta' ? 'instantes' : '~' + eta(l) + ' min') + '. Deixe a câmara fria pronta.']));
  ativas().filter(l => l.h < 2).forEach(l => a.push(['v' + l.id, 'amber', 'fa-hourglass-half', 'O lote #' + l.id + ' vence em ' + hm(l.h) + '. Priorize a triagem e o uso.']));
  const pend = recebidas().filter(l => !l.conf).length;
  if (pend) a.push(['c', 'amber', 'fa-clipboard-question', pend + ' recebimento(s) aguardam a conferência do peso e da condição.']);
  if (e.alvaraDias < 0) a.push(['alv', 'amber', 'fa-file-shield', 'O alvará/registro da instituição está vencido. Atualize os documentos no perfil.']);
  else if (e.alvaraDias < 60) a.push(['alv', 'amber', 'fa-file-shield', 'O alvará/registro da instituição vence em ' + e.alvaraDias + ' dias. Atualize os documentos no perfil.']);
  const cor = { blue: 'bg-blue-50 border-blue-200 text-blue-800', sky: 'bg-sky-50 border-sky-200 text-sky-800', amber: 'bg-amber-50 border-amber-200 text-amber-800' };
  $('alertas').innerHTML = a.filter(x => !descartados.has(x[0])).map(x =>
    '<div class="flex items-center gap-3 px-4 py-3 rounded-2xl border text-sm ' + cor[x[1]] + '"><i class="fa-solid ' + x[2] + '"></i><p class="flex-1">' + esc(x[3]) +
    '</p><button data-k="' + x[0] + '" class="opacity-60 hover:opacity-100" aria-label="Dispensar alerta"><i class="fa-solid fa-xmark"></i></button></div>').join('');
}
$('alertas').onclick = e => { const b = e.target.closest('button[data-k]'); if (b) { descartados.add(b.dataset.k); alertas(); } };

function capacidade() {
  const e = perfilOng(), prev = previstoHoje();
  const pct = Math.min(100, Math.round(prev / (e.capKg || 1) * 100)), frio = ativas().filter(l => l.cons !== 'amb').length;
  $('cap-txt').textContent = fmt(prev) + ' de ' + fmt(e.capKg) + ' kg previstos para hoje (' + pct + '%)';
  $('cap-barra').style.width = pct + '%';
  $('cap-barra').className = 'h-full rounded-full transition-all duration-700 ' + (pct >= 90 ? 'bg-red-500' : pct >= 70 ? 'bg-amber-500' : 'bg-blue-500');
  $('cap-nota').textContent = 'Horário de recebimento: ' + e.horario + '.' + (frio ? ' ' + frio + ' lote(s) exigem controle de temperatura.' : '') + (pct >= 90 ? ' Capacidade quase no limite: avise a administração antes de aceitar mais doações.' : '');
}

/* ---------- Mapa ---------- */
const mapa = L.map('mapa').setView(ORG, 12);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapa);
const ico = (cor, extra, s) => L.divIcon({ className: '', html: '<div class="pin ' + (extra || '') + '" style="background:' + cor + ';' + (s ? 'width:' + s + 'px;height:' + s + 'px' : '') + '"></div>', iconSize: [s || 16, s || 16] });
L.marker(ORG, { icon: ico('#2563eb', '', 22), zIndexOffset: 400 }).addTo(mapa).bindTooltip(perfilOng().fantasia, { permanent: true, direction: 'top' });
let camadas = [], motoristas = {};
function desenharMapa() {
  camadas.forEach(c => mapa.removeLayer(c)); camadas = []; motoristas = {};
  const a = ativas(), pts = [ORG];
  $('sem-rota').classList.toggle('hidden', a.length > 0);
  a.forEach(l => {
    const o = origem(l); pts.push(o);
    camadas.push(L.marker(o, { icon: ico('#0f172a') }).addTo(mapa).bindTooltip(l.emp));
    const linha = L.polyline([o, ORG], { color: '#3b82f6', weight: 3, dashArray: '8 8' }).addTo(mapa);
    camadas.push(linha);
    osRota(o, ORG).then(p => { if (mapa.hasLayer(linha)) { linha.setLatLngs(p); linha.setStyle({ dashArray: null, opacity: 0.7 }); } });
    motoristas[l.id] = L.marker(pos(l), { icon: ico('#f97316', 'pulso'), zIndexOffset: 500 }).addTo(mapa).bindTooltip('#' + l.id + ' · ' + l.mot);
    camadas.push(motoristas[l.id]);
  });
  if (pts.length > 1) mapa.fitBounds(L.latLngBounds(pts).pad(0.25));
}

/* ---------- Chegadas e doações disponíveis ---------- */
function chegadas() {
  const a = ativas();
  $('chegadas').innerHTML = a.length ? a.map(l =>
    '<article class="rounded-2xl border border-slate-200 p-4 hover:border-blue-300 transition-colors cursor-pointer" data-id="' + l.id + '">' +
    '<div class="flex flex-wrap items-start justify-between gap-2"><div><p class="font-bold text-slate-900">#LOTE-' + l.id + ' · ' + esc(l.tipo) + '</p><p class="text-xs text-slate-500">' + esc(l.emp) + ' · ' + fmt(l.kg) + ' kg</p></div>' + pill(ATIVO[l.estado]) + '</div>' +
    '<div class="flex flex-wrap gap-2 mt-3 text-[11px] font-semibold">' +
      (l.cons !== 'amb' ? '<span class="px-2 py-1 rounded-lg bg-sky-50 text-sky-700"><i class="fa-regular fa-snowflake mr-1"></i>' + CONS[l.cons] + '</span>' : '') +
      '<span class="px-2 py-1 rounded-lg ' + (l.h < 2 ? 'bg-red-50 text-red-700' : 'bg-slate-100 text-slate-600') + '"><i class="fa-regular fa-clock mr-1"></i>Validade em ' + hm(l.h) + '</span>' +
      '<span class="px-2 py-1 rounded-lg bg-slate-100 text-slate-600"><i class="fa-solid fa-truck mr-1"></i>' + esc(l.veic) + ' · ' + esc(l.placa) + '</span></div>' +
    '<div class="mt-3"><div class="flex justify-between text-xs font-semibold text-slate-500 mb-1"><span>Motorista ' + esc(l.mot) + '</span><span>' + (l.estado === 'porta' ? 'Na porta' : 'Chega em ~' + eta(l) + ' min') + '</span></div>' +
    '<div class="h-2 bg-slate-100 rounded-full overflow-hidden"><div class="h-full bg-blue-500 rounded-full transition-all" style="width:' + Math.round(l.prog * 100) + '%"></div></div></div></article>').join('')
    : '<p class="text-sm text-slate-500 text-center py-8"><i class="fa-solid fa-mug-hot text-3xl text-slate-300 block mb-2"></i>Nenhuma chegada em andamento. Confira as doações disponíveis na região.</p>';
}
$('chegadas').onclick = e => { const c = e.target.closest('article[data-id]'); if (c) abrir(+c.dataset.id); };

function disponiveis() {
  $('disp').innerHTML = DISP.length ? DISP.map(d => {
    const motivo = d.aceita ? '' : incompativel(d);
    return '<article class="rounded-2xl border border-slate-200 p-4"><div class="flex justify-between gap-2"><p class="font-bold text-sm text-slate-900">' + esc(d.tipo) + '</p><span class="text-xs font-bold text-slate-700 whitespace-nowrap">' + fmt(d.kg) + ' kg</span></div>' +
      '<p class="text-xs text-slate-500 mt-0.5">' + esc(d.emp) + ' · a ' + fmt(d.dist, 1) + ' km</p>' +
      '<div class="flex flex-wrap gap-2 mt-2 text-[11px] font-semibold"><span class="px-2 py-1 rounded-lg ' + (d.cons !== 'amb' ? 'bg-sky-50 text-sky-700' : 'bg-slate-100 text-slate-600') + '">' + CONS[d.cons] + '</span><span class="px-2 py-1 rounded-lg ' + (d.h < 6 ? 'bg-amber-50 text-amber-700' : 'bg-slate-100 text-slate-600') + '">Validade em ' + hm(d.h) + '</span></div>' +
      (d.aceita ? '<p class="mt-3 text-xs font-semibold text-blue-600"><i class="fa-solid fa-check mr-1"></i> Aceita. Aguardando o motorista sair.</p>'
        : (motivo ? '<p class="text-xs font-semibold text-red-600 mt-2"><i class="fa-solid fa-ban mr-1"></i>' + esc(motivo) + '</p>' : '<p class="text-xs font-semibold text-emerald-600 mt-2"><i class="fa-solid fa-circle-check mr-1"></i>Cabe na capacidade e nas categorias da instituição</p>') +
          '<div class="grid grid-cols-2 gap-2 mt-3"><button data-d="' + d.id + '" data-r="aceitar" class="btn-lar !py-2 text-xs" ' + (motivo ? 'disabled' : '') + '>Aceitar</button><button data-d="' + d.id + '" data-r="recusar" class="btn-sec !py-2 text-xs">Recusar</button></div>') + '</article>';
  }).join('') : '<p class="text-sm text-slate-500 text-center py-8">Nenhuma proposta no momento.</p>';
}
$('disp').onclick = e => {
  const b = e.target.closest('button[data-r]'); if (!b) return;
  const d = DISP.find(x => x.id === +b.dataset.d); if (!d || d.aceita) return;
  if (b.dataset.r === 'aceitar') {
    const m = incompativel(d); if (m) return aviso(m + '.');
    b.disabled = true;
    osApi('ong/propostas/' + d.id + '/aceitar', {})
      .then(() => { d.aceita = Date.now(); disponiveis(); capacidade(); aviso('Proposta do lote #' + d.id + ' aceita. A empresa ' + d.emp + ' foi avisada e os motoristas da região já a veem.'); atualizarDados(); })
      .catch(e => { b.disabled = false; aviso(e.message); });
  } else {
    const motivo = prompt('Recusar a proposta do lote #' + d.id + '? Ela será enviada a outra instituição compatível.\nMotivo (opcional):', '');
    if (motivo === null) return;
    b.disabled = true;
    osApi('ong/propostas/' + d.id + '/recusar', { motivo })
      .then(() => { DISP.splice(DISP.indexOf(d), 1); disponiveis(); aviso('Proposta do lote #' + d.id + ' recusada. A plataforma vai oferecê-la a outra instituição.'); atualizarDados(); })
      .catch(e => { b.disabled = false; aviso(e.message); });
  }
};

/* ---------- Código de validação ---------- */
let tokLote = null;
function painelToken() {
  const porta = DOACOES.filter(l => l.estado === 'porta'), sel = $('sel-token');
  sel.classList.toggle('hidden', porta.length < 2);
  sel.innerHTML = porta.map(l => '<option value="' + l.id + '"' + (l.id === tokLote ? ' selected' : '') + '>Lote #' + l.id + ' · ' + esc(l.mot) + '</option>').join('');
  const l = porta.find(x => x.id === tokLote) || porta[0]; tokLote = l ? l.id : null;
  const c = $('t-corpo');
  if (!l) { c.innerHTML = '<div class="text-center py-8 text-sm text-slate-500"><i class="fa-solid fa-door-closed text-3xl text-slate-300"></i><p class="mt-2 font-semibold">Nenhum motorista na porta.</p><p>Quando um chegar, você poderá gerar o código aqui.</p></div>'; return; }
  const t = tokenAtivo(l);
  const info = '<div class="rounded-2xl bg-slate-50 p-3 text-sm mb-4"><p class="font-bold">#LOTE-' + l.id + ' · ' + fmt(l.kg) + ' kg</p><p class="text-xs text-slate-500">' + esc(l.emp) + ' · motorista ' + esc(l.mot) + ' (' + esc(l.placa) + ')</p></div>';
  c.innerHTML = t
    ? info + '<div class="flex justify-center gap-1.5 mb-3" role="text" aria-label="Código ' + t.codigo.split('').join(' ') + '">' + t.codigo.split('').map(ch => '<span class="w-10 h-12 rounded-xl bg-blue-50 border border-blue-200 text-blue-700 grid place-items-center text-xl font-extrabold font-mono">' + ch + '</span>').join('') + '</div>' +
      '<p class="text-center text-xs text-slate-500">Expira em <b id="t-tempo" class="text-slate-800"></b></p>' +
      '<p class="text-center text-xs text-blue-600 font-semibold mt-3"><i class="fa-solid fa-spinner fa-spin mr-1"></i> Aguardando o motorista inserir o código e a foto...</p>' +
      '<div class="grid grid-cols-2 gap-2 mt-4"><button data-acao="copiar" class="btn-sec">Copiar</button><button data-acao="gerar" class="btn-sec">Novo código</button></div>'
    : info + (l.token && !l.token.usado ? '<p class="text-xs font-semibold text-amber-600 mb-3">O código anterior expirou. Gere um novo.</p>' : '') +
      '<button data-acao="gerar" class="btn-lar w-full"><i class="fa-solid fa-key mr-1"></i> Gerar código de validação</button>';
  tempoToken();
}
function tempoToken() {
  const el = $('t-tempo'), t = tokenAtivo(DOACOES.find(x => x.id === tokLote));
  if (!el || !t) return;
  const s = Math.max(0, Math.round((t.expira - Date.now()) / 1000));
  el.textContent = Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
}
$('sel-token').onchange = e => { tokLote = +e.target.value; painelToken(); };
$('t-corpo').onclick = e => {
  const b = e.target.closest('button[data-acao]'); if (!b) return;
  const l = DOACOES.find(x => x.id === tokLote); if (!l) return;
  if (b.dataset.acao === 'gerar') {
    b.disabled = true;
    gerarToken(l).then(painelToken);
  } else {
    const t = tokenAtivo(l); if (!t) return;
    (navigator.clipboard ? navigator.clipboard.writeText(t.codigo) : Promise.reject()).then(() => aviso('Código copiado.'), () => aviso('Código: ' + t.codigo));
  }
};

/* ---------- KPIs, gráficos, parceiros e cadastro ---------- */
function kpis() {
  const p = PER[$('periodo').value]; if (!p) return;
  const ex = DOACOES.filter(l => l.nova && l.estado === 'recebida'), exKg = ex.reduce((s, l) => s + l.kg, 0), kg = p.kg + exKg;
  $('k-delta').textContent = p.delta + ' vs. período anterior';
  contar($('k-kg'), kg, v => fmt(v) + ' kg'); $('k-per').textContent = p.nome;
  contar($('k-ref'), kg * REFEICOES_POR_KG, v => fmt(v)); $('k-fam').textContent = 'Para ' + fmt(perfilOng().familias) + ' famílias atendidas';
  contar($('k-lotes'), p.lotes + ex.length, v => fmt(v));
  contar($('k-emp'), p.empresas, v => fmt(v));
  chart.data.labels = p.labels;
  chart.data.datasets[0].data = p.kgS.map((v, i, a) => i === a.length - 1 ? v + exKg : v);
  chart.update();
}
const chart = new Chart($('recChart'), {
  type: 'bar',
  data: { labels: [], datasets: [{ label: 'Quilos recebidos', data: [], backgroundColor: '#3b82f6', borderRadius: 8, maxBarThickness: 44 }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { color: '#64748b' } }, x: { grid: { display: false }, ticks: { color: '#64748b' } } } }
});
const CATS_REC = OS_ESTADO.categorias || { labels: [], data: [] };
new Chart($('catChart'), {
  type: 'doughnut',
  data: { labels: CATS_REC.labels.length ? CATS_REC.labels : ['Sem recebimentos'], datasets: [{ data: CATS_REC.data.length ? CATS_REC.data : [1], backgroundColor: ['#3b82f6', '#f97316', '#10b981', '#8b5cf6', '#94a3b8'], borderWidth: 0 }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, cutout: '68%' }
});
$('periodo').onchange = kpis;

function parceiros() {
$('parceiros').innerHTML = PARCEIROS.length ? PARCEIROS.map(p =>
  '<div><div class="flex justify-between text-sm mb-1"><span class="font-semibold text-slate-700">' + esc(p[0]) + '</span><span class="font-bold">' + fmt(p[1]) + ' kg</span></div>' +
  '<div class="h-2 bg-slate-100 rounded-full overflow-hidden"><div class="h-full bg-blue-500 rounded-full" style="width:' + Math.round(p[1] / (PARCEIROS[0][1] || 1) * 100) + '%"></div></div></div>').join('')
  : '<p class="text-sm text-slate-500">Ainda sem doações recebidas nos últimos 12 meses.</p>';
}

function cadastro() {
  const e = perfilOng(), lin = (r, v) => '<div class="flex justify-between gap-3"><dt class="text-slate-500">' + r + '</dt><dd class="font-semibold text-right">' + v + '</dd></div>';
  $('cadastro').innerHTML = lin('Situação', '<span class="text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full text-xs font-bold">Aprovado</span>') +
    lin('Alvará / registro', '<span class="' + (e.alvaraDias < 60 ? 'text-amber-600' : 'text-emerald-700') + '">' + (e.alvaraDias < 0 ? 'vencido' : 'vence em ' + e.alvaraDias + ' dias') + '</span>') +
    lin('Responsável', esc(e.resp)) + lin('Famílias atendidas', fmt(e.familias)) + lin('Capacidade diária', fmt(e.capKg) + ' kg') +
    lin('Horário de recebimento', esc(e.horario)) + lin('Endereço de entrega', esc(e.end));
}

/* ---------- Histórico de recebimentos ---------- */
let filtro = 'todas', busca = '', linhas = [];
const CHIPS = [['todas', 'Todas'], ['pendente', 'Aguardando conferência'], ['conferida', 'Conferidas'], ['ocorrencia', 'Com ocorrência']];
function tabela() {
  const q = busca.trim().toLowerCase();
  $('chips').innerHTML = CHIPS.map(c => '<button data-f="' + c[0] + '" class="chip px-3 py-1.5 rounded-full text-xs font-bold border border-slate-200 text-slate-600 hover:bg-slate-50" aria-pressed="' + (filtro === c[0]) + '">' + c[1] +
    ' <span class="opacity-70">' + recebidas().filter(l => c[0] === 'todas' || situacao(l) === c[0]).length + '</span></button>').join('');
  linhas = recebidas().filter(l => (filtro === 'todas' || situacao(l) === filtro) && (!q || ('#lote-' + l.id + ' ' + l.tipo + ' ' + l.emp).toLowerCase().includes(q)));
  $('linhas').innerHTML = linhas.map(l =>
    '<tr class="hover:bg-slate-50/50 cursor-pointer" data-id="' + l.id + '"><td class="py-4 px-6"><p class="font-semibold text-slate-900">#LOTE-' + l.id + '</p><p class="text-[11px] text-slate-400">' + quando(l.dias) + '</p></td>' +
    '<td class="py-4 px-6 text-slate-600">' + esc(l.tipo) + '</td><td class="py-4 px-6 text-slate-600">' + esc(l.emp) + '</td>' +
    '<td class="py-4 px-6 font-medium">' + fmt(l.conf ? l.conf.kg : l.kg) + ' kg' + (l.conf && l.conf.kg !== l.kg ? '<p class="text-[11px] text-slate-400">informado: ' + fmt(l.kg) + ' kg</p>' : '') + '</td>' +
    '<td class="py-4 px-6">' + pill(SIT[situacao(l)]) + '</td>' +
    '<td class="py-4 px-6 text-right no-print"><button class="p-2 bg-slate-100 hover:bg-blue-50 hover:text-blue-600 rounded-xl text-slate-600" aria-label="Ver detalhe do lote ' + l.id + '"><i class="fa-solid fa-eye"></i></button></td></tr>').join('');
  $('vazio').classList.toggle('hidden', linhas.length > 0);
}
$('chips').onclick = e => { const b = e.target.closest('button[data-f]'); if (b) { filtro = b.dataset.f; tabela(); } };
$('busca').oninput = e => { busca = e.target.value; tabela(); };
$('linhas').onclick = e => { const tr = e.target.closest('tr[data-id]'); if (tr) abrir(+tr.dataset.id); };
$('btn-csv').onclick = () => {
  const rows = [['Lote', 'Data', 'Categoria', 'Empresa', 'Peso informado (kg)', 'Peso recebido (kg)', 'Situação', 'Observação'],
    ...linhas.map(l => [l.id, quando(l.dias), l.tipo, l.emp, l.kg, l.conf ? l.conf.kg : '', SIT[situacao(l)][0], l.conf ? l.conf.obs : ''])];
  baixar('recebimentos.csv', '\ufeff' + rows.map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(';')).join('\n'), 'text/csv;charset=utf-8');
};
$('btn-relatorio').onclick = () => window.print();

/* ---------- Detalhe e conferência do recebimento ---------- */
let aberto = null, foco = null;
function detalhe() {
  const l = DOACOES.find(x => x.id === aberto); if (!l) return;
  $('d-titulo').textContent = '#LOTE-' + l.id;
  const rec = l.estado === 'recebida', feitos = rec ? (l.conf ? 5 : 4) : l.estado === 'porta' ? 3 : 2;
  const campo = (r, v) => '<div><p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">' + r + '</p><p class="font-semibold text-slate-800">' + v + '</p></div>';
  const linha = PASSOS.map((p, i) => '<li class="flex items-center gap-3 text-sm ' + (i < feitos ? 'text-slate-800 font-semibold' : 'text-slate-400') + '"><span class="w-6 h-6 rounded-full grid place-items-center text-[10px] ' + (i < feitos ? 'bg-emerald-500 text-white' : 'bg-slate-100') + '"><i class="fa-solid ' + (i < feitos ? 'fa-check' : 'fa-circle text-[5px]') + '"></i></span>' + p + '</li>').join('');
  let extra = '';
  if (rec && !l.conf) extra = '<form id="form-conf" class="rounded-2xl border border-blue-200 bg-blue-50/40 p-4 space-y-3"><p class="font-bold text-sm text-slate-800">Conferência do recebimento</p>' +
    '<div><label class="rotulo" for="c-kg">Peso recebido (kg)</label><input id="c-kg" type="number" min="0" step="0.1" value="' + (l.pesoOrigem ?? l.kg) + '" class="campo"><p class="text-xs text-slate-500 mt-1">Peso na retirada: ' + fmt(l.pesoOrigem ?? l.kg) + ' kg. Diferença acima de ' + PLAT.regras.tolPesoPct + '% gera ocorrência e retém o frete.</p></div>' +
    '<div><label class="rotulo" for="c-cond">Condição dos alimentos</label><select id="c-cond" class="campo"><option value="ok">Tudo em ordem</option><option value="parcial">Parcialmente impróprio</option><option value="improprio">Impróprio para consumo</option></select></div>' +
    '<div><label class="rotulo" for="c-obs">Observação</label><textarea id="c-obs" rows="2" maxlength="300" class="campo" placeholder="Ex: parte das frutas machucada"></textarea></div>' +
    '<button class="btn-lar w-full" type="submit">Salvar conferência</button></form>';
  if (l.conf && l.conf.cond !== 'ok') extra = '<p class="text-sm text-red-700 bg-red-50 rounded-2xl p-4"><i class="fa-solid fa-triangle-exclamation mr-1"></i> Ocorrência registrada: ' + esc(l.conf.obs || 'sem observação') + '</p>';
  if (l.estado === 'porta') extra = '<button data-acao="ir-token" class="btn-lar w-full"><i class="fa-solid fa-key mr-1"></i> Gerar código de validação</button>';
  $('d-conteudo').innerHTML = (rec ? pill(SIT[situacao(l)]) : pill(ATIVO[l.estado])) +
    '<div class="grid grid-cols-2 gap-4">' + campo('Categoria', esc(l.tipo)) + campo('Peso informado', fmt(l.kg) + ' kg') + campo('Empresa doadora', esc(l.emp)) + campo('Conservação', CONS[l.cons]) +
    campo('Motorista', esc(l.mot)) + campo('Veículo', esc(l.veic) + ' · ' + esc(l.placa)) + (l.conf ? campo('Peso recebido', fmt(l.conf.kg) + ' kg') : '') + (rec ? campo('Recebida em', quando(l.dias)) : campo('Validade', 'em ' + hm(l.h))) + '</div>' +
    extra + '<div><p class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">Histórico</p><ol class="space-y-3">' + linha + '</ol></div>' +
    (rec ? '<button data-acao="recibo" class="btn-sec w-full"><i class="fa-solid fa-file-arrow-down mr-1"></i> Baixar recibo de recebimento</button>' : '');
}
function abrir(id) {
  aberto = id; foco = document.activeElement; detalhe();
  $('drawer').classList.remove('invisible', 'translate-x-full'); $('drawer').setAttribute('aria-hidden', 'false');
  $('veu-d').classList.remove('hidden'); $('d-fechar').focus();
}
function fechar() {
  aberto = null; $('drawer').classList.add('invisible', 'translate-x-full'); $('drawer').setAttribute('aria-hidden', 'true');
  $('veu-d').classList.add('hidden'); if (foco) foco.focus();
}
$('d-fechar').onclick = fechar; $('veu-d').onclick = fechar;
document.addEventListener('keydown', e => { if (e.key === 'Escape' && aberto) fechar(); });
$('d-conteudo').onclick = e => {
  const b = e.target.closest('button[data-acao]'); if (!b) return;
  const l = DOACOES.find(x => x.id === aberto);
  if (b.dataset.acao === 'ir-token') { tokLote = l.id; fechar(); painelToken(); $('token').scrollIntoView({ behavior: 'smooth' }); }
  if (b.dataset.acao === 'recibo') baixar('recibo-lote-' + l.id + '.txt', 'RECIBO DE RECEBIMENTO - OngSave Brasil\nInstituição: ' + perfilOng().nome + '\nLote: #' + l.id + '\nCategoria: ' + l.tipo + '\nEmpresa doadora: ' + l.emp + '\nPeso informado: ' + l.kg + ' kg\nPeso recebido: ' + (l.conf ? l.conf.kg + ' kg' : 'a conferir') + '\nValidado por código e foto do motorista ' + l.mot + '.\n(Modelo de exemplo)');
};
$('d-conteudo').addEventListener('submit', e => {
  e.preventDefault();
  const l = DOACOES.find(x => x.id === aberto), kg = +$('c-kg').value, cond = $('c-cond').value;
  if (!(kg >= 0) || $('c-kg').value === '') return aviso('Informe o peso recebido.');
  const bt = e.target.querySelector('button[type=submit]'); if (bt) bt.disabled = true;
  salvarConferencia(l, kg, cond, $('c-obs').value.trim()).then(ok => { if (!ok && bt) bt.disabled = false; detalhe(); tabela(); alertas(); });
});

/* ---------- Atualização ao vivo (dados do servidor) ---------- */
function atualizarTudo() { resumo(); alertas(); capacidade(); chegadas(); disponiveis(); painelToken(); desenharMapa(); tabela(); kpis(); parceiros(); cadastro(); if (aberto) detalhe(); }
iniciarSimulacao(atualizarTudo, () => { ativas().forEach(l => motoristas[l.id] && osMover(motoristas[l.id], pos(l))); chegadas(); capacidade(); });
setInterval(() => {
  const l = DOACOES.find(x => x.id === tokLote);
  if (l && l.estado === 'porta' && l.token && !l.token.usado && l.token.expira <= Date.now() && $('t-tempo')) painelToken(); else tempoToken();
}, 1000);

/* ---------- Início ---------- */
parceiros(); cadastro(); resumo(); alertas(); capacidade(); chegadas(); disponiveis(); painelToken(); desenharMapa(); tabela();
setTimeout(kpis, 600);
