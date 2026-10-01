/* Visão Geral da empresa: KPIs, gráficos, acompanhamento ao vivo, lotes e detalhe. */
$('topo-extra').innerHTML = '<label class="sr-only" for="periodo">Período</label>' +
  '<select id="periodo" class="bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs text-slate-600 font-semibold focus:outline-none focus:ring-2 focus:ring-orange-300">' +
  '<option value="30">Últimos 30 dias</option><option value="90">Últimos 90 dias</option><option value="0">Acumulado do ano</option></select>';

const PER = OS_ESTADO.per || {};
const VEIC = OS_ESTADO.veiculos || [];
function aoEstado(e) { if (e.per) osAtribuir(PER, e.per); if (e.veiculos) osSubstituir(VEIC, e.veiculos); }
if (PER[0]) $('periodo').options[2].textContent = PER[0].nome;

/* ---------- Banner, alertas e KPIs ---------- */
const descartados = new Set();
function resumo() {
  const a = LOTES.filter(ativo).length, w = LOTES.filter(l => l.estado === 'aguardando').length;
  $('banner-resumo').textContent = (a + w === 0)
    ? 'Nenhum lote em andamento. Publique excedentes e conecte-se com motoristas e ONGs da sua região.'
    : a + ' em andamento e ' + w + ' aguardando ONG ou motorista. Publique novos lotes a qualquer momento.';
  $('k-ativos').textContent = a + ' ativos agora';
}
function alertas() {
  const lista = [];
  LOTES.filter(l => l.estado === 'aguardando' && horas(l) < 6).forEach(l => lista.push(['l' + l.id, 'amber', 'fa-triangle-exclamation',
    'Lote #' + l.id + ' vence em ' + hm(horas(l)) + ' e ainda não tem motorista. Ajuste a janela de carga ou o veículo.']));
  LOTES.filter(l => l.estado === 'aguardando' && l.ong === 'A definir').forEach(l => lista.push(['o' + l.id, 'amber', 'fa-hands-holding-child',
    'Nenhuma ONG compatível aceitou o lote #' + l.id + ' ainda. A equipa OngSave foi avisada.']));
  LOTES.filter(l => l.estado === 'aceito' && !l.retirada).forEach(l => lista.push(['r' + l.id, 'amber', 'fa-clipboard-check',
    'O motorista ' + (l.mot || '') + ' chegou para retirar o lote #' + l.id + '. Registre peso e temperatura para liberar a saída.']));
  if (usoPlano().extras) lista.push(['franq', 'amber', 'fa-ticket', 'Você passou da franquia do plano em ' + usoPlano().extras + ' lote(s): taxa extra de R$ ' + fmt(usoPlano().custoExtra) + ' na próxima fatura.']);
  lista.push(['fatura', 'slate', 'fa-file-invoice', 'Próxima cobrança da subscrição em ' + proxCobranca() + ' (R$ ' + fmt(plano().preco, 2) + ').']);
  const cor = { amber: 'bg-amber-50 border-amber-200 text-amber-800', slate: 'bg-white border-slate-200 text-slate-600' };
  $('alertas').innerHTML = lista.filter(a => !descartados.has(a[0])).map(a =>
    '<div class="flex items-center gap-3 px-4 py-3 rounded-2xl border text-sm ' + cor[a[1]] + '"><i class="fa-solid ' + a[2] + '"></i><p class="flex-1">' + esc(a[3]) +
    '</p><button data-k="' + a[0] + '" class="opacity-60 hover:opacity-100" aria-label="Dispensar alerta"><i class="fa-solid fa-xmark"></i></button></div>').join('');
}
$('alertas').onclick = e => { const b = e.target.closest('button[data-k]'); if (b) { descartados.add(b.dataset.k); alertas(); } };

function kpis() {
  const p = PER[$('periodo').value]; if (!p) return;
  $('k-delta').textContent = p.delta + ' vs. período anterior';
  contar($('k-kg'), p.kg, v => fmt(v) + ' kg');
  $('k-ref').textContent = 'Equivale a ' + fmt(p.kg * REFEICOES_POR_KG) + ' refeições';
  contar($('k-co2'), p.kg * CO2_POR_KG / 1000, v => fmt(v, 1) + ' t');
  contar($('k-lotes'), p.lotes, v => fmt(v));
  $('k-plano').textContent = plano().nome;
  const custo = +perfilEmpresa().descarte;
  if (!(custo > 0)) {
    $('k-eco').classList.remove('skel'); $('k-eco').innerHTML = '<a href="perfil#descarte" class="text-base text-orange-600 underline">Informar custo</a>';
    $('k-eco-sub').textContent = 'Informe quanto gasta hoje com descarte';
  } else {
    const eco = (custo - plano().preco) * p.dias / 30;
    contar($('k-eco'), eco, v => (eco < 0 ? '-R$ ' : 'R$ ') + fmt(Math.abs(v)));
    $('k-eco-sub').textContent = 'Seu descarte (R$ ' + fmt(custo) + '/mês) menos o plano';
  }
  painelPlano(); fiscal();
  grafico();
}

/* ---------- Gráficos ---------- */
let metrica = 'kg', chart;
const METR = {
  kg: p => ({ d: p.kgS, l: 'Quilos resgatados' }),
  lotes: p => ({ d: p.loteS, l: 'Lotes' }),
  co2: p => ({ d: p.kgS.map(k => +(k * CO2_POR_KG / 1000).toFixed(2)), l: 'Toneladas de CO₂ evitadas' })
};
function grafico() {
  const p = PER[$('periodo').value]; if (!p) return;
  const m = METR[metrica](p);
  $('g-sub').textContent = p.sub;
  chart.data.labels = p.labels;
  chart.data.datasets[0].data = m.d; chart.data.datasets[0].label = m.l;
  chart.update();
}
chart = new Chart($('esgChart'), {
  type: 'bar',
  data: { labels: [], datasets: [{ data: [], backgroundColor: '#f97316', borderRadius: 8, maxBarThickness: 44 }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { color: '#64748b' } }, x: { grid: { display: false }, ticks: { color: '#64748b' } } } }
});
const veicN = k => (VEIC.find(v => v.k === k) || { n: 0 }).n;
const vehicleChart = new Chart($('vehicleChart'), {
  type: 'doughnut',
  data: { labels: ['Carro / Utilitário', 'Van / Furgão', 'Caminhão de carga'], datasets: [{ data: ['carro', 'van', 'cam'].map(veicN), backgroundColor: ['#f97316', '#0f172a', '#fdba74'], borderWidth: 0 }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, cutout: '72%' }
});
$('metricas').onclick = e => {
  const b = e.target.closest('button[data-m]'); if (!b) return;
  metrica = b.dataset.m;
  document.querySelectorAll('#metricas button').forEach(x => x.setAttribute('aria-pressed', x === b));
  grafico();
};
$('periodo').onchange = kpis;

/* ---------- Acompanhamento ao vivo ---------- */
const mapa = L.map('mapa').setView(EMPRESA, 13);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapa);
const ico = (cor, extra) => L.divIcon({ className: '', html: '<div class="pin ' + (extra || '') + '" style="background:' + cor + '"></div>', iconSize: [16, 16] });
L.marker(EMPRESA, { icon: ico('#0f172a') }).addTo(mapa).bindTooltip('Sua loja (coleta)');
let camadas = [], pinMot = null, sel = null;

/* Posição do motorista: última recebida do GPS; sem ela, estimada pelo progresso. */
const pos = l => l.posLat != null ? [l.posLat, l.posLon] : (o => [EMPRESA[0] + (o[0] - EMPRESA[0]) * l.prog, EMPRESA[1] + (o[1] - EMPRESA[1]) * l.prog])(destino(l));
/* Em trânsito para a ONG ou com o motorista já a caminho da loja (com posição conhecida). */
const aCaminhoDaLoja = l => l.estado === 'aceito' && l.posLat != null;
function listaTransito() {
  const ls = LOTES.filter(l => l.estado === 'transito' || aCaminhoDaLoja(l));
  $('sel-lote').innerHTML = ls.map(l => '<option value="' + l.id + '">Lote #' + l.id + (aCaminhoDaLoja(l) ? ' · motorista a caminho da loja' : ' → ' + esc(l.ong)) + '</option>').join('');
  $('sel-lote').classList.toggle('hidden', ls.length === 0);
  acompanhar(ls.find(l => sel && l.id === sel.id) || ls[0]);
}
function acompanhar(l) {
  camadas.forEach(c => mapa.removeLayer(c)); camadas = []; pinMot = null; sel = l || null;
  $('sem-transito').classList.toggle('hidden', !!l);
  if (!l) { $('t-titulo').textContent = 'Estado do lote'; $('t-info').innerHTML = '<p class="text-slate-500">Selecione um lote na tabela para ver o histórico.</p>'; return; }
  $('sel-lote').value = l.id;
  const o = destino(l);
  camadas.push(L.marker(o, { icon: ico('#3b82f6') }).addTo(mapa).bindTooltip(l.ong, { permanent: true, direction: 'top' }));
  const coleta = aCaminhoDaLoja(l), id = l.id;
  const reta = L.polyline(coleta ? [pos(l), EMPRESA] : [EMPRESA, o], { color: '#f97316', weight: 4, dashArray: '8 8' }).addTo(mapa);
  camadas.push(reta);
  if (coleta) camadas.push(L.polyline([EMPRESA, o], { color: '#94a3b8', weight: 3, dashArray: '4 8' }).addTo(mapa));
  // Rota real pelas ruas (substitui a linha reta quando chega)
  osRota(coleta ? pos(l) : EMPRESA, coleta ? EMPRESA : o).then(pts => {
    if (!sel || sel.id !== id || !mapa.hasLayer(reta)) return;
    reta.setLatLngs(pts); reta.setStyle({ dashArray: null, opacity: 0.85 });
  });
  pinMot = L.marker(pos(l), { icon: ico('#f97316', 'pulso'), zIndexOffset: 500 }).addTo(mapa).bindTooltip(l.mot || 'Motorista');
  camadas.push(pinMot);
  mapa.fitBounds(L.latLngBounds([EMPRESA, o, pos(l)]).pad(0.3));
  painelTrack();
}
function painelTrack() {
  const l = sel; if (!l) return;
  const coleta = aCaminhoDaLoja(l), resta = km(pos(l), coleta ? EMPRESA : destino(l)), eta = Math.max(1, Math.round(resta / 25 * 60));
  $('t-titulo').textContent = 'Lote #' + l.id + ' · ' + fmt(l.kg) + ' kg';
  $('t-info').innerHTML =
    '<div><div class="flex justify-between text-xs font-semibold text-slate-500 mb-1"><span>' + fmt(resta, 1) + (coleta ? ' km até a sua loja' : ' km restantes') + '</span><span>' + (coleta ? 'Motorista chega em ~' : 'Chegada em ~') + eta + ' min</span></div>' +
    '<div class="h-2 bg-slate-100 rounded-full overflow-hidden"><div class="h-full bg-orange-500 rounded-full transition-all" style="width:' + Math.round(l.prog * 100) + '%"></div></div></div>' +
    timeline(l) +
    '<div class="flex items-center gap-3 p-3 rounded-2xl bg-slate-50"><div class="w-9 h-9 rounded-full bg-orange-100 text-orange-600 grid place-items-center"><i class="fa-solid fa-user"></i></div>' +
    '<div><p class="font-semibold">' + esc(l.mot) + '</p><p class="text-xs text-slate-500">' + esc(l.veic) + ' · ' + esc(l.placa) + '</p></div></div>';
}
$('sel-lote').onchange = () => acompanhar(LOTES.find(l => l.id === +$('sel-lote').value));

/* ---------- Tabela de lotes ---------- */
let filtro = 'todos', busca = '';
const CHIPS = [['todos', 'Todos'], ['aguardando', 'Aguardando'], ['aceito', 'A caminho'], ['transito', 'Em trânsito'], ['entregue', 'Entregues'], ['cancelado', 'Cancelados']];
function tabela() {
  const q = busca.trim().toLowerCase();
  $('chips').innerHTML = CHIPS.map(c => {
    const n = c[0] === 'todos' ? LOTES.length : LOTES.filter(l => l.estado === c[0]).length;
    return '<button data-f="' + c[0] + '" class="chip px-3 py-1.5 rounded-full text-xs font-bold border border-slate-200 text-slate-600 hover:bg-slate-50" aria-pressed="' + (filtro === c[0]) + '">' + c[1] + ' <span class="opacity-70">' + n + '</span></button>';
  }).join('');
  const rows = LOTES.filter(l => (filtro === 'todos' || l.estado === filtro) && (!q || ('#' + l.id + ' ' + l.tipo + ' ' + l.ong).toLowerCase().includes(q)));
  $('linhas').innerHTML = rows.map(l =>
    '<tr class="hover:bg-slate-50/50 transition-colors cursor-pointer" data-id="' + l.id + '">' +
    '<td class="py-4 px-6"><p class="font-semibold text-slate-900">#LOTE-' + l.id + '</p><p class="text-[11px] text-slate-400">' + quando(l.dias) + '</p></td>' +
    '<td class="py-4 px-6 text-slate-600">' + esc(l.tipo) + '</td>' +
    '<td class="py-4 px-6 font-medium text-slate-800">' + fmt(l.kg) + ' kg</td>' +
    '<td class="py-4 px-6"><span class="bg-slate-100 text-slate-700 px-2.5 py-1 rounded-lg text-xs font-semibold">' + esc(l.veic) + '</span></td>' +
    '<td class="py-4 px-6 text-slate-600">' + esc(l.ong) + '</td>' +
    '<td class="py-4 px-6">' + badge(l.estado) + '</td>' +
    '<td class="py-4 px-6 text-right"><button class="p-2 bg-slate-100 hover:bg-orange-50 hover:text-orange-600 rounded-xl text-slate-600 transition-colors" aria-label="Ver detalhe do lote ' + l.id + '"><i class="fa-solid fa-eye"></i></button></td></tr>').join('');
  $('vazio').classList.toggle('hidden', rows.length > 0);
}
$('chips').onclick = e => { const b = e.target.closest('button[data-f]'); if (b) { filtro = b.dataset.f; tabela(); } };
$('busca').oninput = e => { busca = e.target.value; tabela(); };
$('linhas').onclick = e => { const tr = e.target.closest('tr[data-id]'); if (tr) abrir(+tr.dataset.id); };

/* ---------- Detalhe do lote ---------- */
let aberto = null, foco = null;
function detalhe() {
  const l = LOTES.find(x => x.id === aberto); if (!l) return;
  $('d-titulo').textContent = '#LOTE-' + l.id;
  const campo = (r, v) => '<div><p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">' + r + '</p><p class="font-semibold text-slate-800">' + v + '</p></div>';
  let acoes = '';
  if (l.estado === 'aguardando') acoes = '<button data-acao="cancelar" class="w-full py-3 rounded-2xl border border-red-200 text-red-600 font-bold text-sm hover:bg-red-50"><i class="fa-solid fa-ban"></i> Cancelar lote</button>';
  if (l.estado === 'aceito') acoes = retiradaHtml(l);
  if (l.retirada && l.estado !== 'aceito') acoes = '<div class="rounded-2xl bg-slate-50 p-4 text-sm"><p class="font-bold text-slate-800">Retirada registrada</p><p class="text-slate-600">' + fmt(l.retirada.kg) + ' kg' + (l.retirada.temp != null ? ' · ' + l.retirada.temp + ' °C' : '') + ' · ' + new Date(l.retirada.em).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) + '</p></div>' + acoes;
  if (l.estado === 'entregue') acoes = (l.retirada ? acoes : '') + '<button data-acao="comprovante" class="w-full py-3 rounded-2xl bg-orange-600 text-white font-bold text-sm hover:bg-orange-700"><i class="fa-solid fa-file-arrow-down"></i> Baixar comprovante</button>';
  $('d-conteudo').innerHTML = badge(l.estado) +
    '<div class="grid grid-cols-2 gap-4">' + campo('Categoria', esc(l.tipo)) + campo('Peso', fmt(l.kg) + ' kg') + campo('Veículo', esc(l.veic)) + campo('Data', quando(l.dias)) +
    campo('ONG destino', esc(l.ong)) + campo('Motorista', l.mot ? esc(l.mot) + ' · ' + esc(l.placa) : 'Ainda não definido') + '</div>' +
    (l.estado === 'aguardando' ? '<p class="text-sm text-amber-700 bg-amber-50 rounded-2xl p-4"><i class="fa-solid fa-clock"></i> Validade em ' + hm(horas(l)) + '. ' +
      (l.ongAceitou ? 'A ONG aceitou: motoristas do raio de ' + PLAT.raioKm + ' km já veem o lote.' : 'Aguardando a ONG aceitar a proposta.') + '</p>' : '') +
    (l.conf ? '<p class="text-sm rounded-2xl p-4 ' + (l.conf.retido ? 'text-red-700 bg-red-50' : 'text-slate-600 bg-slate-50') + '"><i class="fa-solid fa-clipboard-check"></i> Conferido pela ONG: ' + fmt(l.conf.kg, 1) + ' kg' + (l.conf.retido ? ' · ocorrência em análise' : '') + '.</p>' : '') +
    '<a href="' + OS_BASE + '/api/conta/fotos/lote/' + l.id + '" target="_blank" rel="noopener" class="text-xs font-semibold text-orange-600 hover:underline"><i class="fa-regular fa-image"></i> Ver foto do lote</a>' +
    (l.estado === 'entregue' ? '<p class="text-sm text-emerald-700 bg-emerald-50 rounded-2xl p-4"><i class="fa-solid fa-seedling"></i> Impacto: ' + fmt(l.kg * REFEICOES_POR_KG) + ' refeições e ' + fmt(l.kg * CO2_POR_KG / 1000, 2) + ' t de CO₂ evitadas. Validado por Token da ONG.</p>' : '') +
    '<div><p class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">Histórico</p>' + timeline(l) + '</div>' + acoes;
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
  const l = LOTES.find(x => x.id === aberto);
  if (b.dataset.acao === 'cancelar' && confirm('Cancelar o lote #' + l.id + '? Ele deixará de aparecer para os motoristas.')) {
    b.disabled = true;
    osApi('empresa/lotes/' + l.id + '/cancelar', {})
      .then(() => { l.estado = 'cancelado'; aviso('Lote #' + l.id + ' cancelado.'); redesenhar(); atualizarDados(); })
      .catch(er => { b.disabled = false; aviso(er.message); });
  }
  if (b.dataset.acao === 'comprovante') {
    const txt = 'COMPROVANTE DE ENTREGA - OngSave Brasil\nLote: #' + l.id + '\nCategoria: ' + l.tipo + '\nPeso: ' + l.kg + ' kg\nONG: ' + l.ong + '\nMotorista: ' + l.mot + ' (' + l.placa + ')\nValidado por Token da ONG.\nEmpresa: ' + EMP.nome + ' (CNPJ ' + EMP.cnpj + ')\nEmitido em: ' + new Date().toLocaleString('pt-BR');
    const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([txt], { type: 'text/plain;charset=utf-8' })); a.download = 'comprovante-lote-' + l.id + '.txt'; a.click();
  }
};

/* ---------- Atualização ao vivo (dados do servidor, via empresa.js) ---------- */
function redesenhar() { resumo(); alertas(); tabela(); listaTransito(); painelPlano(); fiscal(); if (aberto) detalhe(); }
aoAtualizar(() => { redesenhar(); kpis(); vehicleChart.data.datasets[0].data = ['carro', 'van', 'cam'].map(veicN); vehicleChart.update(); },
  () => {
    if (sel) sel = LOTES.find(l => l.id === sel.id) || sel;
    painelTrack(); if (pinMot && sel) osMover(pinMot, pos(sel));
  });

/* ---------- Franquia, dossiê fiscal e retirada ---------- */
function painelPlano() {
  const u = usoPlano(), pct = Math.min(100, Math.round(u.uso / (u.franquia || 1) * 100));
  $('pl-txt').textContent = u.uso + ' de ' + u.franquia + ' lotes (' + plano().nome + ')';
  $('pl-barra').style.width = pct + '%';
  $('pl-barra').className = 'h-full rounded-full transition-all duration-700 ' + (u.extras ? 'bg-red-500' : pct >= 80 ? 'bg-amber-500' : 'bg-orange-500');
  $('pl-nota').textContent = u.extras
    ? u.extras + ' lote(s) acima da franquia, com taxa de R$ ' + fmt(u.taxa) + ' cada: R$ ' + fmt(u.custoExtra) + ' a mais na próxima fatura. Avalie trocar de plano em Subscrição & Perfil.'
    : 'Dentro da franquia. Cada lote extra custa R$ ' + fmt(u.taxa) + '.';
}
function fiscal() { $('fs-n').textContent = LOTES.filter(l => l.estado === 'entregue').length + ' doação(ões) entregue(s) com comprovante da ONG'; }
$('btn-dossie').onclick = () => {
  const rows = [['Lote', 'Data', 'Categoria', 'Peso na retirada (kg)', 'ONG recebedora', 'Comprovação'],
    ...LOTES.filter(l => l.estado === 'entregue').map(l => [l.id, quando(l.dias), l.tipo, l.retirada ? l.retirada.kg : l.kg, l.ong, 'Validado por código da ONG e foto da descarga'])];
  const csv = '\ufeff' + rows.map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(';')).join('\n');
  const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' })); a.download = 'dossie-doacoes.csv'; a.click();
};
function retiradaHtml(l) {
  if (l.retirada) return '<div class="rounded-2xl border border-orange-200 bg-orange-50/50 p-5 text-center"><p class="text-xs font-bold uppercase tracking-wider text-slate-500">Código de retirada</p>' +
    '<p class="text-3xl font-extrabold font-mono tracking-[.3em] text-orange-700 my-2">' + esc(l.retirada.codigo) + '</p><p class="text-xs text-slate-500">Passe ao motorista. Ele digita no app para iniciar a rota.</p></div>';
  const frio = l.cons !== 'amb';
  return '<form id="form-retirada" class="rounded-2xl border border-orange-200 bg-orange-50/40 p-5 space-y-3"><p class="font-bold text-sm text-slate-800">Confirmar retirada</p>' +
    '<p class="text-xs text-slate-500">Registre o que o motorista está levando. Isso protege a empresa e permite comparar com o peso recebido pela ONG.</p>' +
    '<div><label class="rotulo" for="rt-kg">Peso retirado (kg)</label><input id="rt-kg" type="number" min="0" step="0.1" value="' + l.kg + '" class="campo"></div>' +
    (frio ? '<div><label class="rotulo" for="rt-temp">Temperatura da carga (°C)</label><input id="rt-temp" type="number" step="0.1" class="campo" placeholder="Ex: 4"></div>' : '') +
    '<div><label class="rotulo" for="rt-obs">Observação</label><input id="rt-obs" maxlength="200" class="campo"></div><button type="submit" class="btn-lar w-full">Gerar código de retirada</button></form>';
}
$('d-conteudo').addEventListener('submit', e => {
  e.preventDefault();
  const l = LOTES.find(x => x.id === aberto), kg = $('rt-kg').value, frio = l.cons !== 'amb', t = frio ? $('rt-temp').value : '';
  if (kg === '' || !(+kg > 0)) return aviso('Informe o peso retirado.');
  if (frio && t === '') return aviso('Informe a temperatura da carga refrigerada.');
  const bt = e.target.querySelector('button[type=submit]'); bt.disabled = true;
  osApi('empresa/lotes/' + l.id + '/retirada', { kg, temp: t, obs: $('rt-obs').value.trim() })
    .then(r => { l.retirada = r; aviso('Retirada do lote #' + l.id + ' registada. Passe o código ao motorista.'); detalhe(); alertas(); })
    .catch(er => { bt.disabled = false; aviso(er.message); });
});

/* ---------- Início ---------- */
resumo(); alertas(); tabela(); listaTransito();
setTimeout(kpis, 600);
