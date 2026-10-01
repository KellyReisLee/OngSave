/* Perfil da Instituição: documentos, dados, morada com busca de CEP, capacidade e alterações pendentes. */
const CAMPOS = ['nome', 'fantasia', 'cnpj', 'resp', 'email', 'tel', 'familias', 'cep', 'rua', 'num', 'compl', 'bairro', 'cidade', 'uf', 'capKg', 'horaIni', 'horaFim', 'alvara'];
const CATS = PADRAO.categorias;
const DOCS = OS_ESTADO.docs || [];
let dados = { ...perfilOng() }, base = '';

const iso = ms => new Date(ms).toISOString().slice(0, 10);
const atual = () => ({
  ...Object.fromEntries(CAMPOS.map(k => [k, $(k).value.trim()])),
  camara: $('camara').checked, cats: CATS.filter((c, i) => $('cat' + i).checked)
});
function erro(id, msg) {
  const p = document.querySelector('[data-e="' + id + '"]'); if (p) p.textContent = msg || '';
  const c = $(id); if (c) c.classList.toggle('invalido', !!msg);
}
function sujo() {
  const s = JSON.stringify(atual()) !== base;
  $('btn-salvar').disabled = !s; $('btn-descartar').disabled = !s;
  $('sujo-aviso').classList.toggle('hidden', !s);
}
function preencher() {
  CAMPOS.forEach(k => { $(k).value = k === 'alvara' ? (dados.alvara || iso(Date.now() + dados.alvaraDias * 864e5)) : (dados[k] ?? ''); erro(k, ''); });
  $('camara').checked = !!dados.camaraFria;
  CATS.forEach((c, i) => { $('cat' + i).checked = dados.categorias.includes(c); });
  erro('cats', ''); base = JSON.stringify(atual()); sujo(); pintarAlvara();
}
function pintarAlvara() {
  const d = Math.ceil((new Date($('alvara').value) - Date.now()) / 864e5), b = $('alv-badge');
  if (isNaN(d)) return;
  b.textContent = d < 0 ? 'Alvará vencido' : 'Alvará vence em ' + d + ' dias';
  b.className = 'inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ' + (d < 0 ? 'bg-red-50 text-red-700 border-red-200' : d < 60 ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-emerald-50 text-emerald-700 border-emerald-200');
}

$('cats').innerHTML = CATS.map((c, i) => '<label class="flex items-center gap-3 rounded-xl border border-slate-200 px-4 py-3 text-sm text-slate-700 cursor-pointer hover:border-blue-300"><input id="cat' + i + '" type="checkbox" class="accent-blue-600 w-4 h-4"> ' + esc(c) + '</label>').join('');

/* Máscaras e CEP */
const mascTel = v => {
  v = v.replace(/\D/g, '').slice(0, 11);
  return v.length > 10 ? v.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3')
    : v.length > 6 ? v.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3')
    : v.length > 2 ? v.replace(/(\d{2})(\d*)/, '($1) $2') : v;
};
const mascCep = v => { v = v.replace(/\D/g, '').slice(0, 8); return v.length > 5 ? v.slice(0, 5) + '-' + v.slice(5) : v; };
$('tel').addEventListener('input', () => { $('tel').value = mascTel($('tel').value); });
$('uf').addEventListener('input', () => { $('uf').value = $('uf').value.replace(/[^a-zA-Z]/g, '').toUpperCase(); });
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
$('form-perfil').addEventListener('input', () => { sujo(); pintarAlvara(); });
$('form-perfil').addEventListener('change', () => { sujo(); pintarAlvara(); });
$('btn-descartar').onclick = () => { dados = { ...perfilOng() }; preencher(); };
window.addEventListener('beforeunload', e => { if (!$('btn-salvar').disabled) { e.preventDefault(); e.returnValue = ''; } });

/* Salvar */
$('form-perfil').onsubmit = ev => {
  ev.preventDefault();
  const d = atual(), dig = d.tel.replace(/\D/g, '');
  const e = {
    nome: !d.nome && 'Informe a razão social.', fantasia: !d.fantasia && 'Informe o nome de exibição.', resp: !d.resp && 'Informe o responsável.',
    email: !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.email) && 'Informe um e-mail válido.',
    tel: (dig.length < 10 || dig.length > 11) && 'Informe um telefone com DDD.',
    familias: !(+d.familias >= 1) && 'Informe o número de famílias.',
    cep: d.cep.length !== 9 && 'Informe o CEP completo.', rua: !d.rua && 'Informe o logradouro.', num: !d.num && 'Informe o número.',
    bairro: !d.bairro && 'Informe o bairro.', cidade: !d.cidade && 'Informe a cidade.', uf: d.uf.length !== 2 && 'UF inválida.',
    capKg: !(+d.capKg > 0) && 'Informe a capacidade diária.',
    horaFim: (!d.horaIni || !d.horaFim || d.horaIni >= d.horaFim) && 'O horário final deve ser depois do inicial.',
    alvara: (!d.alvara || new Date(d.alvara) < new Date(iso(Date.now()))) && 'Informe uma validade futura.',
    cats: d.cats.length === 0 && 'Marque ao menos uma categoria.'
  };
  Object.keys(e).forEach(k => erro(k, e[k] || ''));
  const primeiro = Object.keys(e).find(k => e[k]);
  if (primeiro) { const alvo = $(primeiro) || document.querySelector('[data-e="' + primeiro + '"]'); alvo.scrollIntoView({ behavior: 'smooth', block: 'center' }); if (alvo.focus) alvo.focus(); return; }

  const moradaMudou = ['cep', 'rua', 'num', 'compl', 'bairro', 'cidade', 'uf'].some(k => d[k] !== (dados[k] || ''));
  const bt = $('btn-salvar'); bt.disabled = true;
  osApi('ong/perfil', { ...d, categorias: d.cats, camaraFria: d.camara })
    .then(novo => {
      Object.assign(perfilOng(), novo); dados = { ...novo }; preencher();
      $('top-nome').textContent = novo.fantasia; $('top-ini').textContent = novo.fantasia.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
      aviso(moradaMudou ? (novo.geocodificado ? 'Dados atualizados. Motoristas e empresas já usam o novo endereço de recebimento.'
        : 'Dados atualizados. Não foi possível localizar o novo endereço no mapa; confira CEP e número.') : 'Dados atualizados com sucesso.');
    })
    .catch(e => { aviso(e.message); sujo(); });
};

/* Documentos (enviados para análise da administração) */
const DOC_ST = { ok: ['Aprovado', 'text-emerald-700 bg-emerald-50 border-emerald-200'], analise: ['Em análise', 'text-amber-700 bg-amber-50 border-amber-200'], rejeitado: ['Rejeitado', 'text-red-700 bg-red-50 border-red-200'] };
function pintarDocs() {
  $('docs').innerHTML = DOCS.length ? DOCS.map((d, i) =>
    '<li class="flex flex-wrap items-center gap-3 py-3"><div class="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 grid place-items-center"><i class="fa-solid fa-file-lines"></i></div>' +
    '<div class="flex-1 min-w-[180px]"><p class="text-sm font-semibold">' + esc(d.nome) + '</p><p class="text-xs text-slate-500">' + esc(d.arq) + (d.status === 'rejeitado' && d.motivo ? ' · ' + esc(d.motivo) : '') + '</p></div>' +
    '<span class="text-xs font-bold border px-2.5 py-1 rounded-full ' + DOC_ST[d.status][1] + '">' + DOC_ST[d.status][0] + '</span>' +
    '<label class="btn-sec !py-2 !px-3 text-xs cursor-pointer">Enviar novo<input type="file" data-i="' + i + '" accept=".pdf,image/png,image/jpeg" class="sr-only"></label></li>').join('')
    : '<li class="py-3 text-sm text-slate-500">Nenhum documento registado.</li>';
}
$('docs').addEventListener('change', e => {
  const inp = e.target.closest('input[type=file]'); if (!inp) return;
  const f = inp.files[0], d = DOCS[+inp.dataset.i]; if (!f || !d) return;
  if (!/^(application\/pdf|image\/(png|jpeg))$/.test(f.type)) return aviso('Envie um PDF, PNG ou JPG.');
  if (f.size > 5 * 1024 * 1024) return aviso('O arquivo passa de 5 MB.');
  const fd = new FormData(); fd.append('arquivo', f);
  aviso('A enviar "' + d.nome + '"...');
  osApi('conta/documentos/' + d.id, fd)
    .then(r => { osSubstituir(DOCS, r.docs); pintarDocs(); aviso('Documento "' + d.nome + '" enviado para análise da administração.'); })
    .catch(er => aviso(er.message));
});

preencher(); pintarDocs();
