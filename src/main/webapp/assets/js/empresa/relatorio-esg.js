/* Relatórios ESG: período, nível do selo, gráficos, auditoria com busca, CSV e PDF (impressão). */
const ESG = OS_ESTADO.esg || { trim: { labels: [], kgAc: [] }, periodos: {}, categorias: { labels: [], data: [] } };
const TRIM = ESG.trim;
const PERIODOS = ESG.periodos;
[...$('per').options].forEach(o => { if (PERIODOS[o.value]) o.textContent = PERIODOS[o.value].nome; });
const NIVEIS = [['Bronze', 1000, 'text-amber-700'], ['Prata', 3000, 'text-slate-400'], ['Ouro', 5000, 'text-yellow-500'], ['Diamante', 10000, 'text-sky-500']];

// Auditoria: lotes entregues dentro do período escolhido (os totais já vêm calculados pelo servidor).
const entregues = () => { const p = PERIODOS[$('per').value] || {}; return LOTES.filter(l => l.estado === 'entregue' && l.entregueMs >= (p.inicio || 0) && l.entregueMs < (p.fim || Infinity)); };
const kgReal = l => l.conf ? l.conf.kg : l.pesoOrigem != null ? l.pesoOrigem : l.kg;
let busca = '', linhas = [];

function contarKpi(el, alvo, f) {
  if (reduz) { el.textContent = f(alvo); return; }
  const ini = performance.now();
  (function passo(t) {
    const k = Math.min((t - ini) / 600, 1);
    el.textContent = f(alvo * (1 - Math.pow(1 - k, 3)));
    if (k < 1) requestAnimationFrame(passo);
  })(ini);
}

function nivel(totalKg) {
  let atual = null, prox = NIVEIS[0];
  NIVEIS.forEach((n, i) => { if (totalKg >= n[1]) { atual = n; prox = NIVEIS[i + 1] || null; } });
  $('n-nome').textContent = atual ? atual[0] : 'Iniciante';
  $('n-ic').className = 'fa-solid fa-medal ' + (atual ? atual[2] : 'text-slate-300');
  if (!prox) { $('n-falta').textContent = 'Nível máximo alcançado'; $('n-barra').style.width = '100%'; return; }
  const de = atual ? atual[1] : 0;
  $('n-falta').textContent = 'Faltam ' + fmt(Math.max(0, prox[1] - totalKg)) + ' kg para o nível ' + prox[0];
  $('n-barra').style.width = Math.min(100, Math.round((totalKg - de) / (prox[1] - de) * 100)) + '%';
}

function kpis() {
  const k = $('per').value, p = PERIODOS[k]; if (!p) return;
  const kg = p.kg;
  $('h-per').textContent = p.nome + '.';
  contarKpi($('e-kg'), kg, v => fmt(v) + ' kg');
  $('e-ref').textContent = 'Equivalente a ' + fmt(kg * REFEICOES_POR_KG) + ' refeições';
  contarKpi($('e-co2'), kg * CO2_POR_KG / 1000, v => fmt(v, 1) + ' t');
  $('e-ong').textContent = p.ongs + ' instituições';
  contarKpi($('e-lotes'), p.lotes, v => fmt(v) + ' lotes');
  nivel(PERIODOS.tudo ? PERIODOS.tudo.kg : 0);
  tabela();
}

function tabela() {
  const k = $('per').value, q = busca.trim().toLowerCase();
  linhas = entregues().filter(l =>
    !q || ('#lote-' + l.id + ' ' + l.tipo + ' ' + l.ong).toLowerCase().includes(q));
  $('audit').innerHTML = linhas.map(l =>
    '<tr class="hover:bg-slate-50/50"><td class="py-4 px-6"><p class="font-semibold text-slate-900">#LOTE-' + l.id + '</p><p class="text-[11px] text-slate-400">' + quando(l.dias) + '</p></td>' +
    '<td class="py-4 px-6 text-slate-600">' + esc(l.tipo) + '</td><td class="py-4 px-6 font-medium">' + fmt(kgReal(l), 1) + ' kg</td>' +
    '<td class="py-4 px-6 font-medium text-emerald-700">' + fmt(kgReal(l) * CO2_POR_KG / 1000, 2) + ' t</td>' +
    '<td class="py-4 px-6 text-slate-600">' + esc(l.ong) + '</td>' +
    '<td class="py-4 px-6"><span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200"><i class="fa-solid fa-shield-check text-[10px]"></i> Validado por Token</span></td></tr>').join('');
  const vazio = linhas.length === 0;
  $('audit-vazio').classList.toggle('hidden', !vazio);
  $('audit-vazio').textContent = LOTES.length >= 300 ? 'Nenhum lote encontrado entre os 300 mais recentes.' : 'Nenhum lote auditado encontrado.';
}

/* Gráficos */
new Chart($('co2Chart'), {
  type: 'line',
  data: { labels: TRIM.labels, datasets: [{ label: 'Toneladas de CO₂ evitadas', data: TRIM.kgAc.map(k => +(k * CO2_POR_KG / 1000).toFixed(2)),
    borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,.1)', borderWidth: 3, fill: true, tension: .3, pointRadius: 4, pointBackgroundColor: '#10b981' }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { color: '#64748b' } }, x: { grid: { display: false }, ticks: { color: '#64748b' } } } }
});
new Chart($('catChart'), {
  type: 'doughnut',
  data: { labels: ESG.categorias.labels.length ? ESG.categorias.labels : ['Sem entregas'], datasets: [{ data: ESG.categorias.data.length ? ESG.categorias.data : [1], backgroundColor: ['#10b981', '#f97316', '#3b82f6', '#8b5cf6', '#94a3b8'], borderWidth: 0 }] },
  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, cutout: '70%' }
});

/* Controles */
$('per').onchange = kpis;
$('busca').oninput = e => { busca = e.target.value; tabela(); };
$('btn-pdf').onclick = () => window.print();
$('btn-csv').onclick = () => {
  const rows = [['Data', 'Lote', 'Categoria', 'Peso (kg)', 'CO2 evitado (t)', 'Instituição', 'Auditoria'],
    ...linhas.map(l => [quando(l.dias), '#LOTE-' + l.id, l.tipo, String(kgReal(l)).replace('.', ','), (kgReal(l) * CO2_POR_KG / 1000).toFixed(2).replace('.', ','), l.ong, 'Validado por Token'])];
  const csv = '\ufeff' + rows.map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(';')).join('\n');
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  a.download = 'auditoria-esg.csv'; a.click();
};

/* Metodologia: os fatores vêm do administrador; sem fonte informada, o relatório é apenas uma estimativa. */
function metodo() {
  const m = PLAT.metodologia;
  $('metodo').innerHTML = '<li><i class="fa-solid fa-cloud text-emerald-600 w-5"></i> CO₂ evitado = quilos salvos × ' + fmt(m.co2PorKg, 2) + ' kg</li>' +
    '<li><i class="fa-solid fa-utensils text-orange-600 w-5"></i> Refeições = quilos salvos × ' + fmt(m.refeicoesPorKg, 1) + '</li>' +
    '<li><i class="fa-solid fa-shield-halved text-blue-600 w-5"></i> Só entram lotes validados por código da ONG</li>' +
    '<li><i class="fa-solid fa-book text-slate-500 w-5"></i> Fonte: ' + (m.fonte ? esc(m.fonte) : '<b class="text-amber-700">ainda não informada</b>') + '</li>';
  $('aviso-metodo').classList.toggle('hidden', !!m.fonte);
  $('aviso-metodo').innerHTML = '<i class="fa-solid fa-triangle-exclamation mt-0.5"></i><p><b>Números estimados.</b> A plataforma ainda não informou a fonte dos fatores de CO₂ e de refeições. Não publique estes valores como auditados antes de a fonte constar aqui.</p>';
}
metodo();
kpis();
