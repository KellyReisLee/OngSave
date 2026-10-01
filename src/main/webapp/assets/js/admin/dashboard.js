/* Dashboard do administrador: fila de aprovação, contas em risco, ocorrências, operações ao vivo e regras. */
const OPS = OS_ESTADO.ops || [];
const FEED = OS_ESTADO.feed || [];
const SEMANAS = OS_ESTADO.entregasSemana || [0, 0, 0, 0, 0, 0];
const FASES = {
  proposta: ['Aguardando a ONG', 'bg-slate-100 text-slate-600 border-slate-200', 'fa-hourglass-half'],
  aguardando: ['Aguardando motorista', 'bg-slate-100 text-slate-600 border-slate-200', 'fa-hourglass-half'],
  coleta: ['Motorista indo à coleta', 'bg-violet-50 text-violet-700 border-violet-200', 'fa-user-clock'],
  caminho: ['A caminho', 'bg-amber-50 text-amber-700 border-amber-200', 'fa-truck-fast'],
  porta: ['Na porta da ONG', 'bg-blue-50 text-blue-700 border-blue-200', 'fa-door-open']
};
const estadoOp = o => FASES[o.fase] || FASES.aguardando;
const subOps = () => OPS.filter(o => o.fase === 'porta').length + ' na porta da ONG · ' + OPS.filter(o => o.fase === 'aguardando' || o.fase === 'proposta').length + ' sem motorista';
const link = u => '<a href="detalhe-utilizador?id=' + u.id + '" class="font-semibold text-slate-900 hover:text-violet-600">' + esc(u.nome) + '</a>';
const botao = (rot, a, id, cls) => '<button data-a="' + a + '" data-id="' + id + '" class="' + (cls || 'btn-sec') + ' !py-1.5 !px-3 text-xs">' + rot + '</button>';
const dias = u => u.criado === 0 ? 'hoje' : 'há ' + u.criado + ' dia(s)';

/* ---------- Alertas e KPIs ---------- */
function resumo() {
  const ativos = USUARIOS.filter(u => u.status === 'ativo'), pend = pendentes().length;
  const altos = USUARIOS.filter(u => u.status === 'ativo' && risco(u).nivel === 'alto').length, abertas = todasOcorr().filter(o => o.status !== 'resolvida').length;
  $('banner-resumo').textContent = pend + ' cadastro(s) na fila, ' + altos + ' conta(s) ativa(s) em risco alto e ' + abertas + ' ocorrência(s) em aberto. Reserva de liquidez em ' + fmt(cobertura(), 1) + ' meses de fretes.';
  contar($('k-ativos'), ativos.length, v => fmt(v));
  $('k-tipos').textContent = ['empresa', 'motorista', 'ong'].map(t => ativos.filter(u => u.tipo === t).length + ' ' + PLURAL[t].toLowerCase()).join(' · ');
  contar($('k-pend'), pend, v => fmt(v));
  $('k-lotes').classList.remove('skel'); $('k-lotes').textContent = OPS.length;
  $('k-lotes-sub').textContent = subOps();
  const c = cobertura(), st = coberturaSt(c);
  contar($('k-res'), c, v => fmt(v, 1) + ' meses');
  $('k-res-st').textContent = st[0]; $('k-res-st').className = 'text-xs font-bold px-2.5 py-1 rounded-full border ' + st[1];
}
const descartados = new Set();
function alertas() {
  const a = [], pend = pendentes().length, c = cobertura();
  const altos = USUARIOS.filter(u => u.status === 'ativo' && risco(u).nivel === 'alto');
  const abertas = todasOcorr().filter(o => o.status === 'aberta').length;
  if (altos.length) a.push(['risco', 'red', 'fa-triangle-exclamation', altos.length + ' conta(s) ativa(s) passaram do limite das regras: ' + altos.map(u => u.nome).join(', ') + '.']);
  if (abertas) a.push(['oc', 'amber', 'fa-circle-exclamation', abertas + ' ocorrência(s) aberta(s) aguardam análise. Enquanto isso, o frete correspondente fica retido.']);
  if (c < 3) a.push(['res', 'amber', 'fa-scale-balanced', 'A reserva cobre ' + fmt(c, 1) + ' meses de fretes, abaixo da meta de 3. Veja o Financeiro.']);
  if (pend) a.push(['pend', 'violet', 'fa-user-clock', pend + ' cadastro(s) aguardam aprovação.']);
  const cor = { red: 'bg-red-50 border-red-200 text-red-800', amber: 'bg-amber-50 border-amber-200 text-amber-800', violet: 'bg-violet-50 border-violet-200 text-violet-800' };
  $('alertas').innerHTML = a.filter(x => !descartados.has(x[0])).map(x => '<div class="flex items-center gap-3 px-4 py-3 rounded-2xl border text-sm ' + cor[x[1]] + '"><i class="fa-solid ' + x[2] + '"></i><p class="flex-1">' + esc(x[3]) +
    '</p><button data-k="' + x[0] + '" class="opacity-60 hover:opacity-100" aria-label="Dispensar alerta"><i class="fa-solid fa-xmark"></i></button></div>').join('');
}
$('alertas').onclick = e => { const b = e.target.closest('button[data-k]'); if (b) { descartados.add(b.dataset.k); alertas(); } };

/* ---------- Fila de aprovação, contas em risco e padrões ---------- */
function fila() {
  const p = pendentes().slice(0, 4);
  $('fila').innerHTML = p.length ? p.map(u => {
    const ok = u.docs.every(d => d.status === 'ok'), an = u.docs.filter(d => d.status === 'analise').length;
    return '<div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 p-4"><div class="flex-1 min-w-[200px]"><p>' + link(u) + '</p><p class="text-xs text-slate-500 mt-0.5">' + tipoTag(u.tipo) + ' <span class="ml-1">' + esc(dias(u)) + '</span></p>' +
      '<p class="text-xs mt-1 ' + (ok ? 'text-emerald-600' : 'text-amber-600') + '">' + (ok ? 'Documentos conferidos' : an + ' documento(s) em análise') + '</p></div>' +
      '<div class="flex gap-2">' + (ok ? botao('Aprovar', 'aprovar', u.id, 'btn-lar') : '<a href="detalhe-utilizador?id=' + u.id + '" class="btn-sec !py-1.5 !px-3 text-xs">Revisar documentos</a>') + botao('Rejeitar', 'rejeitar', u.id) + '</div></div>';
  }).join('') : '<p class="text-sm text-slate-500 text-center py-8"><i class="fa-solid fa-circle-check text-3xl text-emerald-400 block mb-2"></i>Fila vazia. Nenhum cadastro aguardando.</p>';
}
function contasRisco() {
  const l = USUARIOS.filter(u => ['ativo', 'pendente'].includes(u.status)).map(u => ({ u, r: risco(u) })).filter(x => x.r.nivel !== 'baixo').sort((a, b) => (b.r.nivel === 'alto') - (a.r.nivel === 'alto')).slice(0, 5);
  $('risco').innerHTML = l.length ? l.map(({ u, r }) => '<div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 p-4"><div class="flex-1 min-w-[220px]"><p>' + link(u) + ' <span class="ml-1">' + tipoTag(u.tipo) + '</span></p>' +
    '<ul class="mt-1 text-xs text-slate-500 space-y-0.5">' + r.flags.filter(f => f[0] !== 'info').slice(0, 2).map(f => '<li>• ' + esc(f[1]) + '</li>').join('') + '</ul></div><div class="flex flex-wrap items-center gap-2">' + pillRisco(r.nivel) +
    (r.nivel === 'alto' && u.status === 'ativo' ? botao('Suspender', 'suspender', u.id) + botao('Bloquear', 'bloquear', u.id, 'btn-sec !text-red-600') : '') + '</div></div>').join('')
    : '<p class="text-sm text-slate-500 text-center py-8"><i class="fa-solid fa-shield-check text-3xl text-emerald-400 block mb-2"></i>Nenhuma conta em risco pelas regras atuais.</p>';
}
function pares() {
  if (!PARES.length) { $('pares').innerHTML = '<p class="text-sm text-slate-500">Sem motoristas com 5 ou mais entregas nos últimos 90 dias.</p>'; return; }
  $('pares').innerHTML = PARES.map(p => {
    const alto = p.pct >= PLAT.regras.concentracaoPct;
    return '<div><div class="flex justify-between text-xs mb-1"><span class="font-semibold text-slate-700">' + esc(p.mot) + ' → ' + esc(p.ong) + '</span><b class="' + (alto ? 'text-red-600' : 'text-slate-600') + '">' + p.pct + '%</b></div>' +
      '<div class="h-2 bg-slate-100 rounded-full overflow-hidden"><div class="h-full rounded-full ' + (alto ? 'bg-red-500' : 'bg-violet-500') + '" style="width:' + p.pct + '%"></div></div><p class="text-[11px] text-slate-400 mt-1">' + p.entregas + ' entregas' + (alto ? ' · acima do limite de ' + PLAT.regras.concentracaoPct + '%' : '') + '</p></div>';
  }).join('');
}

/* ---------- Regras de moderação ---------- */
function regras() { $('r-rej').value = PLAT.regras.rejeicaoPct; $('r-min').value = PLAT.regras.minEntregas; $('r-tol').value = PLAT.regras.tolPesoPct; $('r-con').value = PLAT.regras.concentracaoPct; }
$('form-regras').onsubmit = e => {
  e.preventDefault();
  const v = { rejeicaoPct: +$('r-rej').value, minEntregas: +$('r-min').value, tolPesoPct: +$('r-tol').value, concentracaoPct: +$('r-con').value };
  if (!(v.rejeicaoPct >= 1 && v.rejeicaoPct <= 100 && v.minEntregas >= 1 && v.tolPesoPct >= 0 && v.tolPesoPct <= 50 && v.concentracaoPct >= 10 && v.concentracaoPct <= 100)) return aviso('Confira os valores das regras.');
  salvarPlat({ regras: v })
    .then(() => { logar('Alterou parâmetros', 'Plataforma', 'Regras de moderação: rejeição ' + v.rejeicaoPct + '%, mínimo ' + v.minEntregas + ' entregas'); regras(); aviso('Regras salvas. As contas em risco foram recalculadas.'); tudo(); })
    .catch(er => aviso(er.message));
};

/* ---------- Operações, feed, ocorrências e auditoria ---------- */
function ops() {
  $('ops').innerHTML = OPS.length ? OPS.map(o => '<tr><td class="py-4 px-6 font-semibold">#' + o.id + '</td><td class="py-4 px-6 text-slate-600">' + esc(o.emp) + ' → ' + esc(o.ong) + '</td><td class="py-4 px-6 text-slate-600">' + esc(o.mot) + '</td><td class="py-4 px-6">' + pill(estadoOp(o)) + '</td></tr>').join('')
    : '<tr><td colspan="4" class="py-8 text-center text-sm text-slate-500">Nenhuma operação em andamento.</td></tr>';
}
const hora = ms => new Date(ms).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
function feed() {
  $('feed').innerHTML = FEED.length ? FEED.slice(0, 6).map(f => '<li class="flex gap-3"><i class="fa-solid ' + f.ic + ' text-violet-500 mt-0.5"></i><div><p class="text-slate-700">' + esc(f.txt) + '</p><p class="text-[11px] text-slate-400">' + dataHora(f.t) + '</p></div></li>').join('')
    : '<li class="text-sm text-slate-500">Sem atividade nos últimos dias.</li>';
}
function ocorr() {
  const l = todasOcorr().filter(o => o.status !== 'resolvida');
  $('oc-n').textContent = l.length + ' em aberto';
  $('ocorr').innerHTML = l.length ? l.slice(0, 4).map(o => {
    const m = porNome(o.motorista), g = porNome(o.ong);
    return '<div class="rounded-2xl border border-slate-200 p-4"><div class="flex flex-wrap items-start justify-between gap-2"><div><p class="font-bold text-slate-900">' + esc(o.tipo) + ' · lote #' + esc(o.lote) + '</p>' +
      '<p class="text-xs text-slate-500 mt-0.5">' + (g ? '<a class="hover:text-violet-600" href="detalhe-utilizador?id=' + g.id + '">' + esc(o.ong) + '</a>' : esc(o.ong)) + ' · motorista ' + (m ? '<a class="hover:text-violet-600" href="detalhe-utilizador?id=' + m.id + '">' + esc(o.motorista) + '</a>' : esc(o.motorista)) + ' · ' + dataHora(o.t) + '</p></div>' + pill(OC_ST[o.status]) + '</div>' +
      '<p class="text-sm text-slate-600 mt-2">' + esc(o.obs || '') + '</p><div class="flex gap-2 mt-3">' + (o.status === 'aberta' ? botao('Analisar', 'analisar', o.id) : '') + botao('Resolver', 'resolver', o.id, 'btn-lar') + '</div></div>';
  }).join('') : '<p class="text-sm text-slate-500 text-center py-8"><i class="fa-solid fa-circle-check text-3xl text-emerald-400 block mb-2"></i>Nenhuma ocorrência em aberto.</p>';
}
function log() {
  $('log').innerHTML = LOG.slice(0, 6).map(x => '<li class="flex gap-3"><i class="fa-solid fa-clipboard-list text-slate-400 mt-0.5"></i><div><p class="text-slate-700"><b>' + esc(x.acao) + '</b> · ' + esc(x.alvo) + '</p><p class="text-[11px] text-slate-400">' + esc(x.ator) + ' · ' + dataHora(x.t) + (x.extra ? ' · ' + esc(x.extra) : '') + '</p></div></li>').join('');
}

/* ---------- Ações (delegação) ---------- */
async function tratar(e) {
  const b = e.target.closest('button[data-a]'); if (!b) return;
  const a = b.dataset.a, id = +b.dataset.id;
  if (['analisar', 'resolver'].includes(a)) {
    const o = todasOcorr().find(x => x.id === id); if (!o) return;
    try {
      if (a === 'analisar') { await osApi('admin/ocorrencias/' + o.id + '/analisar', {}); o.status = 'analise'; logar('Iniciou análise da ocorrência', '#' + o.id, o.tipo); }
      else {
        const r = await pedirMotivo({ titulo: 'Resolver ocorrência #' + o.id, msg: 'Registre a decisão. O frete retido do motorista é liberado, ajustado pelo peso conferido ou estornado conforme o motivo.',
          motivos: ['Sem irregularidade: frete liberado', 'Frete ajustado pelo peso conferido', 'Frete estornado ao motorista', 'Conta sancionada (frete estornado)', 'Empresa notificada (frete liberado)', 'Outro (frete liberado)'], obrigatorio: true, botao: 'Resolver' });
        if (!r) return;
        await osApi('admin/ocorrencias/' + o.id + '/resolver', r);
        o.status = 'resolvida'; logar('Resolveu a ocorrência', '#' + o.id, r.motivo + ': ' + r.obs); aviso('Ocorrência resolvida e registrada na auditoria.');
      }
    } catch (er) { return aviso(er.message); }
    return tudo();
  }
  const u = porId(id); if (!u) return;
  const ok = await acao(u, { aprovar: 'ativo', rejeitar: 'rejeitado', suspender: 'suspenso', bloquear: 'bloqueado' }[a]);
  if (ok) tudo();
}
['fila', 'risco', 'ocorr'].forEach(i => $(i).addEventListener('click', tratar));

/* ---------- Gráficos ---------- */
const entChart = new Chart($('entChart'), {
  type: 'bar',
  data: { labels: ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4', 'Sem 5', 'Sem 6'], datasets: [{ label: 'Lotes entregues', data: SEMANAS, backgroundColor: '#8b5cf6', borderRadius: 8, maxBarThickness: 44 }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { color: '#64748b' } }, x: { grid: { display: false }, ticks: { color: '#64748b' } } } }
});
const tipoChart = new Chart($('tipoChart'), {
  type: 'doughnut',
  data: { labels: ['Empresas', 'Motoristas', 'ONGs'], datasets: [{ data: [0, 0, 0], backgroundColor: ['#f97316', '#10b981', '#3b82f6'], borderWidth: 0 }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, cutout: '66%' }
});
function graficos() { tipoChart.data.datasets[0].data = ['empresa', 'motorista', 'ong'].map(t => USUARIOS.filter(u => u.tipo === t && u.status === 'ativo').length); tipoChart.update(); }

function tudo() { resumo(); alertas(); fila(); contasRisco(); pares(); ocorr(); log(); graficos(); }
/* ---------- Mapa ao vivo das entregas ---------- */
const mapaOps = L.map('mapa-ops', { scrollWheelZoom: false }).setView([-23.5614, -46.6450], 12);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(mapaOps);
const icoOp = (cor, extra, s) => L.divIcon({ className: '', html: '<div class="pin ' + (extra || '') + '" style="background:' + cor + (s ? ';width:' + s + 'px;height:' + s + 'px' : '') + '"></div>', iconSize: [s || 16, s || 16] });
const noMapa = {};          // id do lote -> { mot, camadas }
let enquadrado = false;
function desenharOps() {
  const vivos = OPS.filter(o => o.posLat != null && (o.fase === 'coleta' || o.fase === 'caminho' || o.fase === 'porta'));
  $('mapa-n').textContent = vivos.length + ' em movimento';
  const ids = new Set(vivos.map(o => o.id));
  Object.keys(noMapa).forEach(id => { if (!ids.has(+id)) { noMapa[id].camadas.forEach(c => mapaOps.removeLayer(c)); delete noMapa[id]; } });
  vivos.forEach(o => {
    const pos = [o.posLat, o.posLon], emp = [o.empLat, o.empLon], ong = [o.ongLat, o.ongLon];
    const dica = '<b>#' + o.id + ' · ' + esc(o.mot) + '</b><br>' + esc(o.emp) + ' → ' + esc(o.ong) + '<br>' + estadoOp(o)[0];
    let n = noMapa[o.id];
    if (n && n.fase !== (o.fase === 'coleta')) { n.camadas.forEach(c => mapaOps.removeLayer(c)); delete noMapa[o.id]; n = null; }
    if (!n) {
      const coleta = o.fase === 'coleta';
      const linha = L.polyline(coleta ? [pos, emp] : [emp, ong], { color: '#8b5cf6', weight: 3, opacity: 0.6, dashArray: '6 8' }).addTo(mapaOps);
      osRota(coleta ? pos : emp, coleta ? emp : ong).then(p => { if (mapaOps.hasLayer(linha)) { linha.setLatLngs(p); linha.setStyle({ dashArray: null }); } });
      const camadas = [linha,
        L.marker(emp, { icon: icoOp('#0f172a') }).addTo(mapaOps).bindTooltip(esc(o.emp)),
        L.marker(ong, { icon: icoOp('#3b82f6') }).addTo(mapaOps).bindTooltip(esc(o.ong))];
      const mot = L.marker(pos, { icon: icoOp('#8b5cf6', 'pulso', 18), zIndexOffset: 600 }).addTo(mapaOps).bindTooltip(dica);
      camadas.push(mot);
      n = noMapa[o.id] = { mot, camadas, fase: coleta };
    } else {
      osMover(n.mot, pos);
      n.mot.setTooltipContent(dica);
    }
  });
  if (!enquadrado && vivos.length) {
    enquadrado = true;
    mapaOps.fitBounds(L.latLngBounds(vivos.flatMap(o => [[o.posLat, o.posLon], [o.empLat, o.empLon], [o.ongLat, o.ongLon]])).pad(0.15));
  }
}

aoMudarAdmin = mudou => {
  desenharOps();
  ops(); feed();
  if (mudou === false) return;      // sem mudanças de estado: não redesenha os gráficos
  tudo(); entChart.data.datasets[0].data = SEMANAS; entChart.update();
};
regras(); ops(); feed(); tudo(); desenharOps();
