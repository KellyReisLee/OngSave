/* Confirmar Entrega: conferir o motorista, gerar o código, acompanhar a validação e conferir o recebimento. */
let sel = null, recente = null; // recente: lote conferido agora, mantido na tela até trocar de lote
const verificados = new Set(); // lotes cujo motorista e placa foram conferidos
const ETAPAS = ['Conferir o motorista', 'Gerar o código', 'Motorista valida', 'Conferir o recebimento'];

const fila = () => DOACOES.filter(l => l.estado === 'porta').concat(DOACOES.filter(l => l.estado === 'caminho'), recebidas().filter(l => !l.conf || l.id === recente));
const atual = () => DOACOES.find(l => l.id === sel);
const hora = ms => new Date(ms).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

function etapa(l) { // índice da etapa em andamento (4 = tudo concluído)
  if (!l) return 0;
  if (l.estado === 'recebida') return l.conf ? 4 : 3;
  if (l.estado === 'porta') return tokenAtivo(l) ? 2 : verificados.has(l.id) ? 1 : 0;
  return 0;
}
function passos() {
  const e = etapa(atual());
  $('passos').innerHTML = ETAPAS.map((p, i) => '<li class="flex items-center gap-3"><span class="w-9 h-9 rounded-full grid place-items-center text-sm font-bold shrink-0 ' +
    (i < e ? 'bg-emerald-500 text-white' : i === e ? 'bg-blue-600 text-white ring-4 ring-blue-100' : 'bg-slate-100 text-slate-400') + '">' + (i < e ? '<i class="fa-solid fa-check"></i>' : i + 1) +
    '</span><span class="text-sm ' + (i <= e ? 'font-bold text-slate-800' : 'text-slate-400') + '">' + p + '</span></li>').join('');
}

function lista() {
  const f = fila();
  if (!f.find(l => l.id === sel)) sel = f.length ? f[0].id : null;
  $('lista').innerHTML = f.length ? f.map(l => {
    const r = l.estado === 'recebida' ? SIT[situacao(l)] : ATIVO[l.estado];
    return '<button data-id="' + l.id + '" aria-pressed="' + (l.id === sel) + '" class="w-full text-left rounded-2xl border-2 p-4 transition-colors ' + (l.id === sel ? 'border-blue-500 bg-blue-50/50' : 'border-slate-200 hover:border-blue-300') + '">' +
      '<div class="flex justify-between gap-2"><p class="font-bold text-sm">#LOTE-' + l.id + '</p><p class="text-xs font-bold text-slate-600">' + fmt(l.kg) + ' kg</p></div>' +
      '<p class="text-xs text-slate-500">' + esc(l.emp) + ' · ' + esc(l.mot) + '</p><div class="mt-2">' + pill(r) + '</div>' +
      (l.estado === 'caminho' ? '<p class="text-[11px] text-slate-500 mt-2">Chega em ~' + eta(l) + ' min</p>' : '') + '</button>';
  }).join('') : '<p class="text-sm text-slate-500 text-center py-8">Nenhum motorista a caminho ou na porta agora.</p>';
}
$('lista').onclick = e => { const b = e.target.closest('button[data-id]'); if (b) { sel = +b.dataset.id; recente = null; todo(); } };

function painel() {
  const l = atual(), c = $('painel');
  if (!l) {
    c.innerHTML = '<div class="text-center py-16 text-slate-500"><i class="fa-solid fa-key text-4xl text-slate-300"></i><h3 class="font-bold text-slate-800 mt-4">Nada para validar agora</h3><p class="text-sm mt-1">Quando um motorista chegar com uma doação, ela aparece na lista ao lado.</p><a href="dashboard" class="btn-sec mt-5">Voltar ao painel</a></div>';
    return;
  }
  const cab = '<div class="pb-5 mb-5 border-b border-slate-100"><p class="text-xs font-bold uppercase tracking-wider text-slate-400">Lote</p><h2 class="text-xl font-extrabold text-slate-900">#LOTE-' + l.id + ' · ' + esc(l.tipo) + '</h2>' +
    '<p class="text-sm text-slate-500">' + esc(l.emp) + ' · ' + fmt(l.kg) + ' kg · ' + CONS[l.cons] + '</p></div>';
  const mot = '<div class="rounded-2xl bg-slate-50 p-4 flex items-center gap-4"><div class="w-12 h-12 rounded-full bg-blue-100 text-blue-600 grid place-items-center text-lg"><i class="fa-solid fa-user"></i></div>' +
    '<div><p class="font-bold">' + esc(l.mot) + '</p><p class="text-sm text-slate-500">' + esc(l.veic) + ' · placa <b class="font-mono text-slate-800">' + esc(l.placa) + '</b></p></div></div>';
  let corpo = '';

  if (l.estado === 'caminho') {
    corpo = mot + '<div class="mt-5 rounded-2xl bg-amber-50 text-amber-800 p-4 text-sm"><i class="fa-solid fa-truck-fast mr-1"></i> O motorista chega em ~' + eta(l) + ' min. Prepare o local de descarga' +
      (l.cons !== 'amb' ? ' e a câmara fria (' + CONS[l.cons].toLowerCase() + ')' : '') + '. Quando ele chegar, você poderá gerar o código.</div>' +
      '<div class="h-2 bg-slate-100 rounded-full overflow-hidden mt-5"><div class="h-full bg-blue-500 rounded-full transition-all" style="width:' + Math.round(l.prog * 100) + '%"></div></div>';
  } else if (l.estado === 'porta') {
    const t = tokenAtivo(l);
    if (t) {
      corpo = mot + '<div class="flex justify-center gap-2 mt-6 mb-3" role="text" aria-label="Código ' + t.codigo.split('').join(' ') + '">' + t.codigo.split('').map(ch =>
        '<span class="w-12 h-16 rounded-2xl bg-blue-50 border-2 border-blue-200 text-blue-700 grid place-items-center text-3xl font-extrabold font-mono">' + ch + '</span>').join('') + '</div>' +
        '<p class="text-center text-sm text-slate-500">Expira em <b id="t-tempo" class="text-slate-800"></b></p>' +
        '<p class="text-center text-sm text-blue-600 font-semibold mt-3"><i class="fa-solid fa-spinner fa-spin mr-1"></i> Aguardando o motorista inserir o código e enviar a foto...</p>' +
        '<div class="grid grid-cols-2 gap-3 mt-5"><button data-acao="copiar" class="btn-sec"><i class="fa-regular fa-copy mr-1"></i> Copiar código</button><button data-acao="gerar" class="btn-sec"><i class="fa-solid fa-rotate mr-1"></i> Gerar novo código</button></div>' +
        '<p class="text-xs text-slate-400 text-center mt-4">Um novo código invalida o anterior.</p>';
    } else {
      const ok = verificados.has(l.id);
      corpo = mot + '<label class="flex gap-3 mt-5 text-sm text-slate-600 cursor-pointer"><input id="ver" type="checkbox" class="mt-1 accent-blue-600 w-4 h-4 shrink-0" ' + (ok ? 'checked' : '') + '>' +
        '<span>Conferi o nome do motorista e a placa do veículo com os dados acima.</span></label>' +
        (l.token && !l.token.usado ? '<p class="text-xs font-semibold text-amber-600 mt-3">O código anterior expirou. Gere um novo.</p>' : '') +
        '<button data-acao="gerar" id="btn-gerar" class="btn-lar w-full mt-4" ' + (ok ? '' : 'disabled') + '><i class="fa-solid fa-key mr-1"></i> Gerar código de validação</button>' +
        '<button data-acao="reportar" class="w-full text-center text-xs font-semibold text-slate-500 hover:text-red-600 mt-4">Algo não confere? Reportar problema</button>';
    }
  } else if (!l.conf) {
    corpo = '<div class="rounded-2xl bg-emerald-50 border border-emerald-200 p-4 flex gap-3 text-emerald-800"><i class="fa-solid fa-circle-check text-2xl"></i><div><p class="font-bold">Entrega validada com sucesso</p>' +
      '<p class="text-sm">O código e a foto do motorista conferem. Agora confira o que foi recebido.</p></div></div>' +
      (l.foto ? '<div class="mt-4 rounded-2xl border border-slate-200 p-4 flex items-center gap-4"><a href="' + OS_BASE + '/api/conta/fotos/entrega/' + l.id + '" target="_blank" rel="noopener" class="shrink-0"><img src="' + OS_BASE + '/api/conta/fotos/entrega/' + l.id + '" alt="Foto da descarga do lote ' + l.id + '" class="w-20 h-20 rounded-xl object-cover bg-slate-100"></a>' +
        '<div class="text-sm"><p class="font-bold">Foto da descarga</p><p class="text-slate-500">Enviada às ' + hora(l.foto.em) + ' · tirada a ' + l.foto.dist + ' m da instituição</p></div></div>' : '') +
      '<form id="form-conf" class="mt-5 rounded-2xl border border-blue-200 bg-blue-50/40 p-5 space-y-4"><p class="font-bold text-slate-800">Conferência do recebimento</p>' +
      '<div class="grid sm:grid-cols-2 gap-4"><div><label class="rotulo" for="c-kg">Peso recebido (kg)</label><input id="c-kg" type="number" min="0" step="0.1" value="' + (l.pesoOrigem ?? l.kg) + '" class="campo"><p class="text-xs text-slate-500 mt-1">Peso na retirada: ' + fmt(l.pesoOrigem ?? l.kg) + ' kg. Diferença acima de ' + PLAT.regras.tolPesoPct + '% gera ocorrência e retém o frete.</p></div>' +
      '<div><label class="rotulo" for="c-cond">Condição dos alimentos</label><select id="c-cond" class="campo"><option value="ok">Tudo em ordem</option><option value="parcial">Parcialmente impróprio</option><option value="improprio">Impróprio para consumo</option></select></div></div>' +
      '<div><label class="rotulo" for="c-obs">Observação</label><textarea id="c-obs" rows="2" maxlength="300" class="campo" placeholder="Ex: parte das frutas chegou machucada"></textarea></div>' +
      '<button type="submit" class="btn-lar w-full">Salvar conferência</button></form>';
  } else {
    corpo = '<div class="text-center py-6"><i class="fa-solid fa-clipboard-check text-5xl text-emerald-500"></i><h3 class="font-extrabold text-lg mt-3">Recebimento conferido</h3>' +
      '<p class="text-sm text-slate-500 mt-1">' + fmt(l.conf.kg) + ' kg registrados' + (l.conf.cond !== 'ok' ? ' com ocorrência, enviada à plataforma' : ', tudo em ordem') + '.</p>' +
      '<div class="flex flex-wrap justify-center gap-3 mt-5"><button data-acao="recibo" class="btn-sec"><i class="fa-solid fa-file-arrow-down mr-1"></i> Baixar recibo</button><a href="dashboard" class="btn-lar">Voltar ao painel</a></div></div>';
  }
  c.innerHTML = cab + corpo;
  tempo();
}
function tempo() {
  const el = $('t-tempo'), t = tokenAtivo(atual());
  if (!el || !t) return;
  const s = Math.max(0, Math.round((t.expira - Date.now()) / 1000));
  el.textContent = Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0');
}

$('painel').onclick = e => {
  const b = e.target.closest('button[data-acao]'); if (!b) return;
  const l = atual(); if (!l) return;
  if (b.dataset.acao === 'gerar') {
    if (!verificados.has(l.id)) return aviso('Confira o motorista e a placa antes de gerar o código.');
    b.disabled = true;
    gerarToken(l).then(todo);
  } else if (b.dataset.acao === 'copiar') {
    const t = tokenAtivo(l); if (!t) return;
    (navigator.clipboard ? navigator.clipboard.writeText(t.codigo) : Promise.reject()).then(() => aviso('Código copiado.'), () => aviso('Código: ' + t.codigo));
  } else if (b.dataset.acao === 'reportar') {
    const texto = prompt('Reportar problema com o motorista ou a carga do lote #' + l.id + '? O código não será gerado.\nDescreva o que não confere:', '');
    if (texto !== null)
      osApi('ong/lotes/' + l.id + '/reportar', { texto }).then(() => { aviso('Problema com o lote #' + l.id + ' reportado à plataforma. Aguarde o contato da administração.'); atualizarDados(); }, e => aviso(e.message));
  } else if (b.dataset.acao === 'recibo') {
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob(['RECIBO DE RECEBIMENTO - OngSave Brasil\nInstituição: ' + perfilOng().nome + '\nLote: #' + l.id + '\nCategoria: ' + l.tipo + '\nEmpresa doadora: ' + l.emp + '\nPeso informado: ' + l.kg + ' kg\nPeso recebido: ' + l.conf.kg + ' kg\nValidado por código e foto do motorista ' + l.mot + '.\n(Modelo de exemplo)'], { type: 'text/plain;charset=utf-8' }));
    a.download = 'recibo-lote-' + l.id + '.txt'; a.click();
  }
};
$('painel').addEventListener('change', e => { if (e.target.id === 'ver') { e.target.checked ? verificados.add(sel) : verificados.delete(sel); $('btn-gerar').disabled = !e.target.checked; passos(); } });
$('painel').addEventListener('submit', e => {
  e.preventDefault();
  const l = atual(), kg = $('c-kg').value, cond = $('c-cond').value;
  if (kg === '' || !(+kg >= 0)) return aviso('Informe o peso recebido.');
  const bt = e.target.querySelector('button[type=submit]'); if (bt) bt.disabled = true;
  recente = l.id;
  salvarConferencia(l, +kg, cond, $('c-obs').value.trim()).then(ok => { if (!ok && bt) bt.disabled = false; todo(); });
});

function ultimas() {
  $('ultimas').innerHTML = recebidas().slice(0, 5).map(l =>
    '<tr><td class="py-4 px-6"><p class="font-semibold text-slate-900">#LOTE-' + l.id + '</p><p class="text-[11px] text-slate-400">' + quando(l.dias) + '</p></td>' +
    '<td class="py-4 px-6 text-slate-600">' + esc(l.emp) + '</td><td class="py-4 px-6 font-medium">' + fmt(l.conf ? l.conf.kg : l.kg) + ' kg</td><td class="py-4 px-6">' + pill(SIT[situacao(l)]) + '</td></tr>').join('');
}
function todo() { lista(); passos(); painel(); ultimas(); }

iniciarSimulacao(todo, () => { lista(); const l = atual(); if (l && l.estado === 'caminho') painel(); });
setInterval(() => {
  const l = atual();
  if (l && l.estado === 'porta' && l.token && !l.token.usado && l.token.expira <= Date.now() && $('t-tempo')) { passos(); painel(); } else tempo();
}, 1000);
todo();
