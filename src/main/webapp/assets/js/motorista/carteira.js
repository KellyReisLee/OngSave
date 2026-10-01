/* OngSave · motorista/carteira: extrato e saque (dados do servidor). */
const MOVS = OS_ESTADO.movimentos || [];
const saldo = () => MOVS.filter(m => !m.retido).reduce((s, m) => s + m.v, 0);

function desenhar() {
  $('saldo').textContent = brl(saldo());
  $('extrato').innerHTML = MOVS.map(m =>
    '<li class="flex items-center justify-between py-3"><div>' +
    '<p class="text-sm font-semibold">' + esc(m.desc) + '</p>' +
    '<p class="text-xs text-slate-500">' + quando(m.dias) + (m.retido ? ' · <b class="text-amber-600">Em análise</b>' : '') + '</p></div>' +
    '<span class="font-bold ' + (m.retido ? 'text-amber-600' : m.v >= 0 ? 'text-emerald-600' : 'text-slate-700') + '">' +
    (m.retido ? '' : m.v >= 0 ? '+ ' : '− ') + brl(Math.abs(m.v)) + '</span></li>').join('');
}

$('form-saque').onsubmit = e => {
  e.preventDefault();
  const v = Math.round(+$('valor').value * 100) / 100;
  if (!(v > 0)) return aviso('Informe um valor maior que zero.', 'erro');
  if (v > saldo() + 0.001) return aviso('O valor passa do saldo disponível.', 'erro');
  const bt = e.target.querySelector('button'); if (bt) bt.disabled = true;
  osApi('motorista/sacar', { valor: v })
    .then(() => osApi('motorista/estado'))
    .then(st => { osSubstituir(MOVS, st.movimentos); desenhar(); e.target.reset(); aviso('Saque de ' + brl(v) + ' solicitado. O pagamento é processado pelo provedor parceiro.'); })
    .catch(er => aviso(er.message, 'erro'))
    .finally(() => { if (bt) bt.disabled = false; });
};
desenhar();

function aoEstado(e) { if (e.movimentos) { osSubstituir(MOVS, e.movimentos); desenhar(); } }
