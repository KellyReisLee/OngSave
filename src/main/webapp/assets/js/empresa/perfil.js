/* Subscrição e Perfil: plano, faturas, dados da empresa com máscaras, busca de CEP e alterações pendentes. */
const CAMPOS = ['nome', 'cnpj', 'email', 'tel', 'descarte', 'cep', 'rua', 'num', 'compl', 'bairro', 'cidade', 'uf'];
let dados = { ...perfilEmpresa() }, base = '', escolhido = null;
const FATURAS = OS_ESTADO.faturas || [];

const atual = () => Object.fromEntries(CAMPOS.map(k => [k, $(k).value.trim()]));
function erro(id, msg) {
  const p = document.querySelector('[data-e="' + id + '"]'); if (p) p.textContent = msg || '';
  $(id).classList.toggle('invalido', !!msg);
}
function sujo() {
  const s = JSON.stringify(atual()) !== base;
  $('btn-salvar').disabled = !s; $('btn-descartar').disabled = !s;
  $('sujo-aviso').classList.toggle('hidden', !s);
}
function preencher() {
  CAMPOS.forEach(k => { $(k).value = dados[k] || ''; erro(k, ''); });
  base = JSON.stringify(atual()); sujo();
}

/* Máscaras */
const mascTel = v => {
  v = v.replace(/\D/g, '').slice(0, 11);
  return v.length > 10 ? v.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3')
    : v.length > 6 ? v.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3')
    : v.length > 2 ? v.replace(/(\d{2})(\d*)/, '($1) $2') : v;
};
const mascCep = v => { v = v.replace(/\D/g, '').slice(0, 8); return v.length > 5 ? v.slice(0, 5) + '-' + v.slice(5) : v; };
$('tel').addEventListener('input', () => { $('tel').value = mascTel($('tel').value); });
$('uf').addEventListener('input', () => { $('uf').value = $('uf').value.replace(/[^a-zA-Z]/g, '').toUpperCase(); });

/* CEP: preenche o endereço pelo ViaCEP */
$('cep').addEventListener('input', async () => {
  $('cep').value = mascCep($('cep').value);
  if ($('cep').value.length !== 9) { $('cep-st').textContent = ''; return; }
  $('cep-st').textContent = 'Buscando endereço...';
  try {
    const r = await (await fetch('https://viacep.com.br/ws/' + $('cep').value.replace('-', '') + '/json/')).json();
    if (r.erro) { $('cep-st').textContent = 'CEP não encontrado.'; return; }
    $('rua').value = r.logradouro || $('rua').value;
    $('bairro').value = r.bairro || ''; $('cidade').value = r.localidade || ''; $('uf').value = r.uf || '';
    $('cep-st').textContent = 'Endereço preenchido. Confira o número.';
  } catch (e) { $('cep-st').textContent = 'Não foi possível consultar o CEP agora.'; }
  sujo();
});
$('form-perfil').addEventListener('input', sujo);
$('btn-descartar').onclick = () => { dados = { ...perfilEmpresa() }; preencher(); };
window.addEventListener('beforeunload', e => { if (!$('btn-salvar').disabled) { e.preventDefault(); e.returnValue = ''; } });

$('form-perfil').onsubmit = ev => {
  ev.preventDefault();
  const d = atual(), digitos = d.tel.replace(/\D/g, '');
  const e = {
    nome: !d.nome && 'Informe a razão social.',
    email: !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.email) && 'Informe um e-mail válido.',
    tel: (digitos.length < 10 || digitos.length > 11) && 'Informe um telefone com DDD.',
    descarte: d.descarte !== '' && !(+d.descarte >= 0) && 'Informe um valor válido.',
    cep: d.cep.length !== 9 && 'Informe o CEP completo.',
    rua: !d.rua && 'Informe o logradouro.', num: !d.num && 'Informe o número.',
    bairro: !d.bairro && 'Informe o bairro.', cidade: !d.cidade && 'Informe a cidade.',
    uf: d.uf.length !== 2 && 'UF inválida.'
  };
  Object.keys(e).forEach(k => erro(k, e[k] || ''));
  const primeiro = Object.keys(e).find(k => e[k]);
  if (primeiro) { $(primeiro).focus(); return; }

  const moradaMudou = ['cep', 'rua', 'num', 'compl', 'bairro', 'cidade', 'uf'].some(k => d[k] !== (dados[k] || ''));
  $('btn-salvar').disabled = true;
  osApi('empresa/perfil', d).then(novo => {
    Object.assign(perfilEmpresa(), novo); dados = { ...novo }; preencher();
    $('top-nome').textContent = novo.nome; $('top-cnpj').textContent = novo.cnpj;
    $('top-ini').textContent = novo.nome.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
    aviso(moradaMudou ? (novo.geocodificado ? 'Dados atualizados. A localização de coleta foi recalculada pela nova morada.'
      : 'Dados atualizados. Não foi possível localizar a nova morada no mapa; confira CEP e número.') : 'Dados atualizados com sucesso.');
  }).catch(er => { aviso(er.message); sujo(); });
};

/* Plano e faturas */
function pintarPlano() {
  const p = plano();
  $('p-nome').textContent = p.nome;
  $('p-desc').textContent = 'Investimento mensal: R$ ' + fmt(p.preco, 2) + ' · ' + p.ex;
  $('p-cobr').textContent = proxCobranca();
  const u = usoPlano();
  $('p-eco').textContent = u.uso + ' de ' + u.franquia + ' lotes' + (u.extras ? ' (' + u.extras + ' extra: R$ ' + fmt(u.custoExtra) + ')' : '');
  $('p-ret').innerHTML = '<i class="fa-solid fa-award text-orange-500 mr-1"></i> Retorno do plano: ' + esc(p.ret) + '. Inclui ' + p.franquia + ' lotes por mês; cada lote extra custa R$ ' + fmt(p.taxaExtra) + '.';
  $('pill-txt').textContent = 'Subscrição B2B Ativa (' + p.nome + ')';
}
const ST_FAT = { paga: ['Paga', 'text-emerald-700 bg-emerald-50 border-emerald-200'], aberta: ['Em aberto', 'text-amber-700 bg-amber-50 border-amber-200'], atrasada: ['Atrasada', 'text-red-700 bg-red-50 border-red-200'] };
const refTexto = ref => new Date(ref + '-15T12:00:00').toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
function faturas() {
  $('faturas').innerHTML = FATURAS.length ? FATURAS.map(f => {
    const st = ST_FAT[f.status] || ST_FAT.aberta, nomePlano = (PLANOS[f.plano] || {}).nome || f.plano;
    return '<li class="flex items-center justify-between gap-3 py-3"><div><p class="text-sm font-semibold capitalize">' + refTexto(f.ref) + '</p><p class="text-xs text-slate-500">R$ ' + fmt(f.valor + f.extras, 2) + ' · ' + esc(nomePlano) + (f.extras ? ' + lotes extras' : '') + '</p></div>' +
      '<div class="flex items-center gap-3"><span class="text-xs font-bold border px-2.5 py-1 rounded-full ' + st[1] + '">' + st[0] + '</span>' +
      '<button data-ref="' + f.ref + '" class="btn-sec !py-2 !px-3" aria-label="Baixar fatura de ' + refTexto(f.ref) + '"><i class="fa-solid fa-file-arrow-down"></i></button></div></li>';
  }).join('') : '<li class="py-3 text-sm text-slate-500">Ainda não há faturas.</li>';
}
$('faturas').onclick = e => {
  const b = e.target.closest('button[data-ref]'); if (!b) return;
  const f = FATURAS.find(x => x.ref === b.dataset.ref); if (!f) return;
  const txt = 'FATURA OngSave Brasil\nEmpresa: ' + dados.nome + '\nCNPJ: ' + dados.cnpj + '\nReferência: ' + f.ref + '\nPlano: ' + ((PLANOS[f.plano] || {}).nome || f.plano) +
    '\nMensalidade: R$ ' + fmt(f.valor, 2) + '\nLotes extras: R$ ' + fmt(f.extras, 2) + '\nTotal: R$ ' + fmt(f.valor + f.extras, 2) + '\nSituação: ' + (ST_FAT[f.status] || ST_FAT.aberta)[0] + '\nEmitido em: ' + new Date().toLocaleString('pt-BR');
  const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([txt], { type: 'text/plain;charset=utf-8' })); a.download = 'fatura-' + f.ref + '.txt'; a.click();
};

/* Troca de plano */
function cartoesPlano() {
  $('planos').innerHTML = Object.entries(PLANOS).map(([k, p]) =>
    '<button type="button" data-k="' + k + '" aria-pressed="' + (escolhido === k) + '" class="text-left rounded-2xl border-2 p-5 transition-colors ' + (escolhido === k ? 'border-orange-500 bg-orange-50' : 'border-slate-200 hover:border-orange-300') + '">' +
    '<p class="font-extrabold text-slate-900">' + p.nome + '</p><p class="text-xs text-slate-500 mb-3">' + p.ex + '</p>' +
    '<p class="text-2xl font-extrabold text-orange-600">R$ ' + fmt(p.preco) + '<span class="text-xs font-semibold text-slate-400">/mês' + (k === 'grande' ? ' ou mais' : '') + '</span></p>' +
    '<p class="text-xs text-slate-600 mt-2">' + p.franquia + ' lotes por mês · lote extra R$ ' + fmt(p.taxaExtra) + '</p><p class="text-xs text-slate-500 mt-1">' + esc(p.ret) + '</p></button>').join('');
}
function modal(abrir) {
  $('modal').classList.toggle('hidden', !abrir);
  if (abrir) { escolhido = EMP.plano; cartoesPlano(); $('m-x').focus(); } else $('btn-plano').focus();
}
$('btn-plano').onclick = () => modal(true);
['m-x', 'm-cancel'].forEach(id => { $(id).onclick = () => modal(false); });
$('modal').onclick = e => { if (e.target === $('modal')) modal(false); };
document.addEventListener('keydown', e => { if (e.key === 'Escape' && !$('modal').classList.contains('hidden')) modal(false); });
$('planos').onclick = e => { const b = e.target.closest('button[data-k]'); if (b) { escolhido = b.dataset.k; cartoesPlano(); } };
$('m-ok').onclick = () => {
  if (escolhido === EMP.plano) return modal(false);
  $('m-ok').disabled = true;
  osApi('empresa/plano', { plano: escolhido })
    .then(() => osApi('empresa/estado'))
    .then(e => { EMP.plano = escolhido; osSubstituir(FATURAS, e.faturas); pintarPlano(); faturas(); modal(false); aviso('Plano alterado para ' + plano().nome + '.'); })
    .catch(er => aviso(er.message))
    .finally(() => { $('m-ok').disabled = false; });
};

preencher(); pintarPlano(); faturas();
