/* OngSave · motorista/histórico: entregas concluídas (vindas do servidor). */
const DADOS = OS_ESTADO.historico || [];
const cls = { 'Paga': 'bg-emerald-100 text-emerald-700', 'Em análise': 'bg-amber-100 text-amber-700', 'Rejeitada': 'bg-red-100 text-red-700' };
let visiveis = [];

function filtrar() {
  const est = $('f-estado').value, dias = +$('f-dias').value, q = $('f-busca').value.trim().toLowerCase();
  visiveis = DADOS.filter(d => (!est || d.estado === est) && (!dias || d.dias <= dias) &&
    (!q || (d.lote + ' ' + d.empresa + ' ' + d.ong).toLowerCase().includes(q)));
  $('linhas').innerHTML = visiveis.map(d =>
    '<tr><td class="py-3 font-bold">#' + d.lote + '</td><td>' + quando(d.dias) + '</td>' +
    '<td>' + esc(d.empresa) + ' → ' + esc(d.ong) + '</td><td>' + d.kg + ' kg</td>' +
    '<td>' + String(d.km).replace('.', ',') + ' km</td><td>' + brl(d.frete) + '</td>' +
    '<td><span class="badge ' + cls[d.estado] + '">' + d.estado + '</span></td></tr>').join('');
  $('vazio').classList.toggle('hidden', visiveis.length > 0);
  const soma = k => visiveis.reduce((s, d) => s + d[k], 0);
  $('t-frete').textContent = brl(soma('frete'));
  $('t-kg').textContent = soma('kg').toLocaleString('pt-BR') + ' kg';
  $('t-km').textContent = soma('km').toFixed(1).replace('.', ',') + ' km';
}
['f-estado', 'f-dias'].forEach(id => { $(id).onchange = filtrar; });
$('f-busca').oninput = filtrar;

$('btn-csv').onclick = () => {
  const linhas = [['Lote', 'Data', 'Empresa', 'ONG', 'Peso (kg)', 'Distância (km)', 'Frete (R$)', 'Estado'],
    ...visiveis.map(d => [d.lote, quando(d.dias), d.empresa, d.ong, d.kg,
      String(d.km).replace('.', ','), d.frete.toFixed(2).replace('.', ','), d.estado])];
  const csv = '\ufeff' + linhas.map(l => l.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(';')).join('\n');
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  a.download = 'historico-entregas.csv';
  a.click();
};
filtrar();
