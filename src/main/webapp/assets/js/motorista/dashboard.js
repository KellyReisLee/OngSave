/* Visão Geral: gráficos e resumo */
const GRAF = OS_ESTADO.graficos || { ganhos: { labels: [], data: [] }, cargas: { labels: [], data: [] } };
new Chart($('c-ganhos'), {
  type: 'bar',
  data: { labels: GRAF.ganhos.labels, datasets: [{ data: GRAF.ganhos.data, backgroundColor: '#10b981', borderRadius: 6 }] },
  options: { maintainAspectRatio: false, plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, ticks: { callback: v => 'R$ ' + v } } } }
});
new Chart($('c-cargas'), {
  type: 'doughnut',
  data: { labels: GRAF.cargas.labels.length ? GRAF.cargas.labels : ['Sem entregas'],
    datasets: [{ data: GRAF.cargas.data.length ? GRAF.cargas.data : [1], backgroundColor: ['#10b981', '#f97316', '#3b82f6', '#8b5cf6', '#94a3b8'], borderWidth: 0 }] },
  options: { maintainAspectRatio: false, cutout: '65%', plugins: { legend: { position: 'bottom' } } }
});

document.querySelectorAll('.veic').forEach(e => { e.textContent = VEICULO; });

(async function () {
  iniciarGps();
  const [ofertas, ativa] = await Promise.all([buscarOfertas(), obterAtiva()]);
  document.querySelectorAll('.n-ofertas').forEach(e => { e.textContent = ofertas.length; });
  if (ativa) $('banner-ativa').classList.remove('hidden');
})();
window.addEventListener('online-mudou', () => location.reload());

/* Últimas entregas e alertas (dados do servidor). */
const CLS_EST = { 'Paga': 'bg-emerald-100 text-emerald-700', 'Em análise': 'bg-amber-100 text-amber-700', 'Rejeitada': 'bg-red-100 text-red-700' };
const ULT = (OS_ESTADO.historico || []).slice(0, 5);
$('ultimas').innerHTML = ULT.length ? ULT.map(d => '<tr><td class="py-3 font-bold">#' + d.lote + '</td><td>' + esc(d.empresa) + ' → ' + esc(d.ong) + '</td><td>' + d.kg + ' kg</td><td>' +
  String(d.km).replace('.', ',') + ' km</td><td>' + brl(d.frete) + '</td><td><span class="badge ' + CLS_EST[d.estado] + '">' + d.estado + '</span></td></tr>').join('')
  : '<tr><td colspan="6" class="py-6 text-center text-slate-500">Ainda sem entregas concluídas.</td></tr>';
(function alertas() {
  const a = [], d = MOT.cnhValidade ? Math.ceil((new Date(MOT.cnhValidade) - Date.now()) / 864e5) : null;
  if (d !== null && d < 0) a.push(['fa-triangle-exclamation text-red-500', 'A sua CNH está vencida. Atualize a validade em Perfil e Veículo para aceitar entregas.']);
  else if (d !== null && d < 60) a.push(['fa-triangle-exclamation text-amber-500', 'Sua CNH vence em ' + d + ' dias. Atualize em Perfil e Veículo.']);
  if (!MOT.consentimento) a.push(['fa-location-dot text-amber-500', 'Para aceitar entregas é preciso dar o consentimento de localização.']);
  const retidos = (OS_ESTADO.movimentos || []).filter(m => m.retido).length;
  if (retidos) a.push(['fa-scale-balanced text-amber-500', retidos + ' frete(s) em análise pela administração.']);
  (OS_ESTADO.notifs || []).slice(0, 3).forEach(n => a.push([n.ic + ' text-emerald-500', n.t]));
  $('alertas').innerHTML = a.length ? a.map(x => '<li class="flex gap-3"><i class="fa-solid ' + x[0] + ' mt-1"></i><span>' + esc(x[1]) + '</span></li>').join('')
    : '<li class="text-slate-500">Nenhum alerta.</li>';
})();
