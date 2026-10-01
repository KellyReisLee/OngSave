/* Financeiro: receita, reserva de liquidez, cobertura de fretes, simulador, parâmetros e movimentações. */
const TIPO_MOV = { mensalidade: 'Mensalidade B2B', extra: 'Lote extra', frete: 'Frete', saque: 'Saque de motorista', reserva: 'Reserva' };
const ST_MOV = { disponivel: 'Concluída', pago: 'Concluída', retido: 'Retido em análise', estornado: 'Estornado', solicitado: 'Saque em processamento',
  paga: 'Concluída', aberta: 'Em aberto', atrasada: 'Atrasada' };
let filtro = 'todas', busca = '', linhasAtuais = [];

/* Movimentações reais: faturas das empresas (3 meses), fretes e saques dos motoristas, e o aporte do mês à reserva. */
function transacoes() {
  const l = [], y = new Date().getFullYear(), m = new Date().getMonth();
  FIN.faturas.forEach(f => {
    const nomePlano = (PLAT.planos[f.plano] || { nome: f.plano }).nome;
    l.push({ t: f.t, tipo: 'mensalidade', desc: 'Mensalidade · ' + f.emp + ' (' + nomePlano + ')', valor: f.valor, st: ST_MOV[f.status] || f.status });
    if (f.extras > 0) l.push({ t: f.t, tipo: 'extra', desc: 'Lotes acima da franquia · ' + f.emp, valor: f.extras, st: ST_MOV[f.status] || f.status });
  });
  FIN.movimentos.forEach(x => l.push(x.tipo === 'saque'
    ? { t: x.t, tipo: 'saque', desc: 'Saque via Pix · ' + x.mot, valor: x.valor, st: ST_MOV[x.status] || x.status }
    : { t: x.t, tipo: 'frete', desc: 'Frete · lote #' + x.lote + ' · ' + x.mot, valor: -x.valor, st: ST_MOV[x.status] || x.status }));
  l.push({ t: new Date(y, m, 1).getTime(), tipo: 'reserva', desc: 'Aporte à reserva de liquidez (' + PLAT.reservaPct + '% da receita do mês)', valor: recSerie()[5] * PLAT.reservaPct / 100, st: 'Concluída' });
  return l.sort((a, b) => b.t - a.t);
}
const retidos = () => FIN.movimentos.filter(x => x.tipo === 'frete' && x.status === 'retido').reduce((s, x) => s + x.valor, 0);

function kpis() {
  const rec = recSerie()[5], c = cobertura(), st = coberturaSt(c);
  contar($('k-rec'), rec, v => brl(Math.round(v)));
  $('k-rec-sub').textContent = brl(mrr()) + ' de mensalidades + ' + brl(extrasMes()) + ' de lotes extras';
  contar($('k-res'), reservaSaldo(), v => brl(Math.round(v)));
  $('k-res-sub').textContent = PLAT.reservaPct + '% de ' + brl(recSerie().reduce((s, v) => s + v, 0)) + ' recebidos em 6 meses';
  contar($('k-cob'), c, v => fmt(v, 1) + ' meses');
  $('k-cob-st').textContent = st[0]; $('k-cob-st').className = 'text-xs font-bold px-2.5 py-1 rounded-full border ' + st[1];
  contar($('k-fre'), FIN.fretes[5], v => brl(Math.round(v)));
  $('k-fre-sub').textContent = brl(retidos()) + ' retidos em análise de ocorrências';
}
function reserva() {
  const saldo = reservaSaldo(), alvo = 3 * fretesMedia(), falta = Math.max(0, alvo - saldo), aporte = recSerie()[5] * PLAT.reservaPct / 100;
  $('res-pct').textContent = PLAT.reservaPct;
  $('res-barra').style.width = (alvo > 0 ? Math.min(100, Math.round(saldo / alvo * 100)) : 100) + '%';
  $('res-barra').className = 'h-full rounded-full transition-all duration-700 ' + (cobertura() >= 3 ? 'bg-emerald-500' : cobertura() >= 2 ? 'bg-amber-500' : 'bg-red-500');
  $('res-esq').textContent = 'Saldo: ' + brl(Math.round(saldo)); $('res-dir').textContent = 'Meta (3 meses de fretes): ' + brl(Math.round(alvo));
  $('res-status').textContent = falta ? 'Faltam ' + brl(Math.round(falta)) + ' para a meta' : 'Meta atingida';
  $('res-status').className = 'text-sm font-semibold ' + (falta ? 'text-amber-600' : 'text-emerald-600');
  $('res-nota').textContent = falta
    ? 'No ritmo atual de aportes (' + brl(Math.round(aporte)) + ' por mês), a meta chega em cerca de ' + (aporte > 0 ? fmt(falta / aporte, 1) : '—') + ' mês(es), desde que os fretes não cresçam. Como os fretes crescem junto com a operação, acompanhe esta tela todo mês.'
    : 'A reserva cobre 3 meses de fretes na média dos últimos 3 meses. Reavalie se o volume de fretes crescer.';
}
function simulador() {
  const rec = +$('sim-rec').value, pct = +$('sim-pct').value;
  if (!(rec > 0 && pct > 0)) { $('sim-out').textContent = 'Informe valores válidos.'; return; }
  const meses = 3 * (pct / 100) / (PLAT.reservaPct / 100), sobra = rec * (1 - PLAT.reservaPct / 100 - pct / 100);
  $('sim-out').innerHTML = 'Com fretes em <b>' + fmt(pct) + '%</b> da receita e <b>' + PLAT.reservaPct + '%</b> indo para a reserva, ela só cobre 3 meses de fretes depois de <b>~' + fmt(meses, 1) + ' meses</b> de operação (sem crescimento). ' +
    'Sobra por mês para servidor, suporte, tarifas e seguros: <b class="' + (sobra < 0 ? 'text-red-600' : '') + '">' + brl(Math.round(sobra)) + '</b>' + (sobra < 0 ? '. Nesse cenário a conta não fecha.' : '.');
}
['sim-rec', 'sim-pct'].forEach(i => $(i).addEventListener('input', simulador));

function graficos() {
  const rec = recSerie(); let ac = 0;
  finChart.data.labels = mesesFin();
  finChart.data.datasets[0].data = rec; finChart.data.datasets[1].data = FIN.fretes;
  finChart.data.datasets[2].data = rec.map(v => Math.round(ac += v * PLAT.reservaPct / 100));
  finChart.update();
  const total = mrr() || 1;
  $('planos').innerHTML = Object.keys(PLAT.planos).map(k => {
    const n = empresasAtivas().filter(u => u.plano === k).length, v = n * PLAT.planos[k].preco;
    return '<div><div class="flex justify-between text-sm mb-1"><span class="font-semibold text-slate-700">' + esc(PLAT.planos[k].nome) + ' <span class="text-slate-400 font-normal">(' + n + ')</span></span><b>' + brl(v) + '</b></div><div class="h-2 bg-slate-100 rounded-full overflow-hidden"><div class="h-full bg-violet-500 rounded-full" style="width:' + Math.round(v / total * 100) + '%"></div></div></div>';
  }).join('');
}
const finChart = new Chart($('finChart'), {
  data: { labels: [], datasets: [
    { type: 'bar', label: 'Entradas', data: [], backgroundColor: '#8b5cf6', borderRadius: 6, maxBarThickness: 28 },
    { type: 'bar', label: 'Fretes pagos', data: [], backgroundColor: '#cbd5e1', borderRadius: 6, maxBarThickness: 28 },
    { type: 'line', label: 'Reserva acumulada', data: [], borderColor: '#10b981', backgroundColor: '#10b981', tension: 0.3, pointRadius: 3 }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, scales: { y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { color: '#64748b', callback: v => 'R$ ' + fmt(v) } }, x: { grid: { display: false }, ticks: { color: '#64748b' } } } }
});

/* Parâmetros */
function parametros() {
  $('p-raio').value = PLAT.raioKm; $('p-res').value = PLAT.reservaPct;
  ['carro', 'camionete', 'van', 'caminhao'].forEach(k => { $('f-' + k).value = PLAT.fretes[k]; });
  $('param-planos').innerHTML = Object.keys(PLAT.planos).map(k => '<tr><td class="py-2 pr-4 font-semibold">' + esc(PLAT.planos[k].nome) + '</td>' +
    ['preco', 'franquia', 'taxaExtra'].map(c => '<td class="py-2 pr-4"><input id="pl-' + k + '-' + c + '" type="number" min="0" value="' + PLAT.planos[k][c] + '" class="campo !mt-0 !w-32" aria-label="' + esc(PLAT.planos[k].nome) + ' ' + c + '"></td>').join('') + '</tr>').join('');
  $('m-co2').value = PLAT.metodologia.co2PorKg; $('m-ref').value = PLAT.metodologia.refeicoesPorKg; $('m-fonte').value = PLAT.metodologia.fonte;
}
$('form-param').onsubmit = e => {
  e.preventDefault();
  const n = id => +$(id).value, fretes = {}, planos = {};
  ['carro', 'camionete', 'van', 'caminhao'].forEach(k => { fretes[k] = n('f-' + k); });
  Object.keys(PLAT.planos).forEach(k => { planos[k] = { preco: n('pl-' + k + '-preco'), franquia: n('pl-' + k + '-franquia'), taxaExtra: n('pl-' + k + '-taxaExtra') }; });
  const ok = n('p-raio') >= 1 && n('p-res') >= 1 && n('p-res') <= 100 && Object.values(fretes).every(v => v > 0) && Object.values(planos).every(p => p.preco > 0 && p.franquia >= 1 && p.taxaExtra >= 0) && n('m-co2') >= 0 && n('m-ref') >= 0;
  if (!ok) return aviso('Confira os valores: preços, franquias e fretes precisam ser positivos.');
  salvarPlat({ raioKm: n('p-raio'), reservaPct: n('p-res'), fretes, planos, metodologia: { co2PorKg: n('m-co2'), refeicoesPorKg: n('m-ref'), fonte: $('m-fonte').value.trim() } })
    .then(() => { logar('Alterou parâmetros', 'Plataforma', 'Raio ' + PLAT.raioKm + ' km, reserva ' + PLAT.reservaPct + '%, planos e fretes'); parametros(); aviso('Parâmetros salvos. Empresas, motoristas e ONGs passam a usar os novos valores.'); tudo(); })
    .catch(er => aviso(er.message));
};

/* Movimentações */
const CHIPS = [['todas', 'Todas'], ['mensalidade', 'Mensalidades'], ['extra', 'Lotes extras'], ['frete', 'Fretes'], ['saque', 'Saques'], ['reserva', 'Reserva']];
function tabela() {
  const l = transacoes(), q = busca.trim().toLowerCase();
  $('chips').innerHTML = CHIPS.map(c => '<button data-f="' + c[0] + '" class="chip px-3 py-1.5 rounded-full text-xs font-bold border border-slate-200 text-slate-600 hover:bg-slate-50" aria-pressed="' + (filtro === c[0]) + '">' + c[1] + ' <span class="opacity-70">' + l.filter(x => c[0] === 'todas' || x.tipo === c[0]).length + '</span></button>').join('');
  linhasAtuais = l.filter(x => (filtro === 'todas' || x.tipo === filtro) && (!q || x.desc.toLowerCase().includes(q)));
  $('linhas').innerHTML = linhasAtuais.map(x => '<tr class="hover:bg-slate-50/50"><td class="py-4 px-6 text-slate-500">' + data(x.t) + '</td><td class="py-4 px-6 text-slate-700">' + esc(x.desc) + '</td><td class="py-4 px-6 text-slate-500">' + TIPO_MOV[x.tipo] + '</td>' +
    '<td class="py-4 px-6 text-right font-bold ' + (x.tipo === 'reserva' ? 'text-violet-600' : x.valor < 0 ? 'text-slate-700' : 'text-emerald-600') + '">' + (x.tipo === 'reserva' ? '↔ ' : x.valor > 0 ? '+ ' : '') + brl(Math.round(x.valor)) + '</td>' +
    '<td class="py-4 px-6">' + (x.st === 'Concluída' ? '<span class="text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full">Concluída</span>' : '<span class="text-xs font-bold text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-full">' + esc(x.st) + '</span>') + '</td></tr>').join('');
  $('vazio').classList.toggle('hidden', linhasAtuais.length > 0);
}
$('chips').onclick = e => { const b = e.target.closest('button[data-f]'); if (b) { filtro = b.dataset.f; tabela(); } };
$('busca').oninput = e => { busca = e.target.value; tabela(); };
$('btn-csv').onclick = () => baixar('movimentacoes.csv', csv([['Data', 'Descrição', 'Tipo', 'Valor (R$)', 'Situação'], ...linhasAtuais.map(x => [data(x.t), x.desc, TIPO_MOV[x.tipo], x.valor.toFixed(2).replace('.', ','), x.st])]), 'text/csv;charset=utf-8');

function tudo() { kpis(); reserva(); graficos(); tabela(); simulador(); }
parametros();
$('sim-rec').value = Math.round(recSerie()[5]) || 10000; $('sim-pct').value = recSerie()[5] > 0 ? Math.round(fretesMedia() / recSerie()[5] * 100) || 30 : 30;
tudo();
aoMudarAdmin = () => tudo();
