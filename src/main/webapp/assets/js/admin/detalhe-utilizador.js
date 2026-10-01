/* Detalhe do utilizador: dados, revisão de documentos, risco, ações com justificativa, notas e histórico. */
const u = porId(new URLSearchParams(location.search).get('id'));
const linha = (r, v) => '<div><dt class="text-[11px] font-bold uppercase tracking-wider text-slate-400">' + r + '</dt><dd class="font-semibold text-slate-800 mt-0.5">' + v + '</dd></div>';
const DOC_ST = { ok: ['Aprovado', 'bg-emerald-50 text-emerald-700 border-emerald-200', 'fa-circle-check'], analise: ['Em análise', 'bg-amber-50 text-amber-700 border-amber-200', 'fa-hourglass-half'], rejeitado: ['Rejeitado', 'bg-red-50 text-red-700 border-red-200', 'fa-xmark'] };

function cabecalho() {
  const r = risco(u);
  $('cab').innerHTML = '<div class="flex flex-wrap items-center gap-5"><div class="w-16 h-16 rounded-2xl bg-violet-100 text-violet-600 grid place-items-center text-xl font-extrabold">' + iniciais(u.nome) + '</div>' +
    '<div class="flex-1 min-w-[220px]"><h2 class="text-xl font-extrabold text-slate-900">' + esc(u.nome) + '</h2><p class="text-sm text-slate-500 mt-0.5">' + esc(u.doc) + ' · cadastro em ' + quando(u.criado) + ' · último acesso ' + (u.ultimo == null ? 'nunca' : u.ultimo === 0 ? 'hoje' : 'há ' + u.ultimo + ' dia(s)') + '</p></div>' +
    '<div class="flex flex-wrap items-center gap-2">' + tipoTag(u.tipo) + pill(ST[u.status]) + pillRisco(r.nivel) + '</div></div>';
  const rec = recomendacao(u);
  $('reco').classList.toggle('hidden', !rec);
  $('reco').innerHTML = '<i class="fa-solid fa-triangle-exclamation mt-0.5"></i><div><p class="font-bold">Ação recomendada: ' + esc(rec) + '</p><p class="text-xs mt-0.5">A conta ultrapassou as regras automáticas. A decisão final é sua e fica registrada na auditoria.</p></div>';
}
function dados() {
  const base = [linha('E-mail', esc(u.email)), linha('Telefone', esc(u.tel)), linha('Cidade', esc(u.cidade))];
  const pl = PLAT.planos[u.plano] || { nome: u.plano || '-', preco: 0, franquia: 0 };
  const extra = u.tipo === 'empresa' ? [linha('Responsável', esc(u.resp || '-')), linha('Plano', esc(pl.nome) + ' · ' + brl(pl.preco) + '/mês'), linha('Franquia de lotes', pl.franquia + ' por mês')]
    : u.tipo === 'motorista' ? [linha('Veículo', esc(u.veiculo) + ' · ' + esc(u.placa)), linha('CNH categoria', esc(u.cnhCat || '-')), linha('Validade da CNH', u.cnhDias == null ? '-' : u.cnhDias < 0 ? '<span class="text-red-600">vencida há ' + (-u.cnhDias) + ' dia(s)</span>' : u.cnhDias < 30 ? '<span class="text-amber-600">vence em ' + u.cnhDias + ' dias</span>' : 'em ' + u.cnhDias + ' dias')]
    : [linha('Responsável legal', esc(u.resp || '-')), linha('Famílias atendidas', fmt(u.familias || 0)), linha('Capacidade diária', fmt(u.capKg || 0) + ' kg'), linha('Validade do alvará', u.alvaraDias == null ? '-' : u.alvaraDias < 0 ? '<span class="text-red-600">vencido</span>' : u.alvaraDias < 30 ? '<span class="text-amber-600">vence em ' + u.alvaraDias + ' dias</span>' : 'em ' + u.alvaraDias + ' dias')];
  $('dados').innerHTML = base.concat(extra).join('');
}
function docs() {
  $('docs').innerHTML = u.docs.map((d, i) => '<li class="flex flex-wrap items-center gap-3 py-3"><div class="w-10 h-10 rounded-xl bg-violet-50 text-violet-600 grid place-items-center"><i class="fa-solid fa-file-lines"></i></div>' +
    '<div class="flex-1 min-w-[180px]"><p class="text-sm font-semibold">' + esc(d.nome) + '</p><p class="text-xs text-slate-500">' + (d.arquivo ? '<a class="text-violet-600 hover:underline" target="_blank" rel="noopener" href="' + OS_BASE + '/api/admin/documentos/' + d.id + '/arquivo">' + esc(d.arq) + '</a>' : esc(d.arq) + ' · sem arquivo enviado') + (d.motivo ? ' · ' + esc(d.motivo) : '') + '</p></div>' + pill(DOC_ST[d.status]) +
    '<div class="flex gap-2">' + (d.status !== 'ok' ? '<button data-d="' + i + '" data-a="ok" class="btn-lar !py-1.5 !px-3 text-xs">Aprovar</button>' : '') + (d.status !== 'rejeitado' ? '<button data-d="' + i + '" data-a="rejeitado" class="btn-sec !py-1.5 !px-3 text-xs">Rejeitar</button>' : '') + '</div></li>').join('');
}
$('docs').onclick = async e => {
  const b = e.target.closest('button[data-d]'); if (!b) return;
  const d = u.docs[+b.dataset.d];
  try {
    if (b.dataset.a === 'ok') {
      await osApi('admin/documentos/' + d.id, { status: 'ok' });
      d.status = 'ok'; d.motivo = null; logar('Aprovou documento', u.nome, d.nome, u.id);
    } else {
      const r = await pedirMotivo({ titulo: 'Rejeitar ' + d.nome, msg: 'O motivo é mostrado ao usuário para que ele envie um novo arquivo.', motivos: ['Ilegível', 'Vencido', 'Dados não conferem', 'Documento inválido', 'Outro'], obrigatorio: true, botao: 'Rejeitar' });
      if (!r) return;
      const res = await osApi('admin/documentos/' + d.id, { status: 'rejeitado', motivo: r.motivo, obs: r.obs });
      d.status = 'rejeitado'; d.motivo = res.motivo; logar('Rejeitou documento', u.nome, d.nome + ' · ' + r.motivo + ': ' + r.obs, u.id);
    }
  } catch (er) { return aviso(er.message); }
  tudo();
};
$('btn-pedir').onclick = async () => {
  const r = await pedirMotivo({ titulo: 'Solicitar documento', msg: 'O usuário será avisado para enviar o arquivo.', motivos: DOCS[u.tipo].concat(['Outro']), botao: 'Solicitar' });
  if (!r) return;
  try {
    await osApi('admin/usuarios/' + u.id + '/solicitar', { documento: r.motivo, obs: r.obs });
    const d = u.docs.find(x => x.nome === r.motivo); if (d) { d.status = 'rejeitado'; d.motivo = 'Novo envio solicitado'; }
    logar('Solicitou documento', u.nome, r.motivo + (r.obs ? ': ' + r.obs : ''), u.id); aviso('Solicitação registrada e enviada ao usuário.'); tudo();
  } catch (er) { aviso(er.message); }
};

function acoes() {
  const ok = u.docs.every(d => d.status === 'ok'), b = (rot, a, cls) => '<button data-a="' + a + '" class="' + (cls || 'btn-sec') + ' w-full">' + rot + '</button>';
  let h = '';
  if (u.status === 'pendente') h = b('Aprovar cadastro', 'ativo', 'btn-lar') .replace('class="', ok ? 'class="' : 'disabled class="') + (ok ? '' : '<p class="text-xs text-amber-600">Aprove todos os documentos para liberar a aprovação.</p>') + b('Rejeitar cadastro', 'rejeitado');
  else if (u.status === 'ativo') h = b('Suspender conta', 'suspenso') + b('Bloquear conta', 'bloqueado', 'btn-sec !text-red-600');
  else if (u.status === 'suspenso') h = b('Reativar conta', 'ativo', 'btn-lar') + b('Bloquear conta', 'bloqueado', 'btn-sec !text-red-600');
  else if (u.status === 'bloqueado') h = b('Reativar conta', 'ativo', 'btn-lar');
  else h = b('Reabrir análise', 'pendente');
  $('acoes').innerHTML = h + '<p class="text-xs text-slate-400">Suspensão e bloqueio pedem justificativa e podem ser contestados pelo usuário.</p>';
}
$('acoes').onclick = async e => {
  const b = e.target.closest('button[data-a]'); if (!b || b.disabled) return;
  const a = b.dataset.a;
  if (a === 'pendente') { if (await acao(u, 'pendente')) tudo(); return; }
  if (a === 'ativo' && u.status !== 'pendente' && !confirm('Reativar a conta de ' + u.nome + '?')) return;
  if (await acao(u, a)) tudo();
};

function metricas() {
  const m = (r, v) => '<div class="flex justify-between gap-3"><dt class="text-slate-500">' + r + '</dt><dd class="font-semibold text-right">' + v + '</dd></div>';
  $('metricas').innerHTML = m('Entregas', fmt(u.st.entregas)) + m('Quilos movimentados', fmt(u.st.kg) + ' kg') + m('Avaliação média', u.st.aval ? fmt(u.st.aval, 1) + ' de 5' : '-') +
    (u.tipo !== 'empresa' ? m('Taxa de rejeição', u.st.rej + '%') : m('Lotes acima da franquia', u.st.extras)) + m('Ocorrências (30 dias)', u.st.ocorr);
}
function sinais() {
  const r = risco(u), cor = { alto: 'text-red-600', medio: 'text-amber-600', info: 'text-slate-500' }, ic = { alto: 'fa-triangle-exclamation', medio: 'fa-circle-exclamation', info: 'fa-circle-info' };
  $('sinais').innerHTML = r.flags.length ? r.flags.map(f => '<li class="flex gap-2 ' + cor[f[0]] + '"><i class="fa-solid ' + ic[f[0]] + ' mt-0.5"></i><span>' + esc(f[1]) + '</span></li>').join('') : '<li class="flex gap-2 text-emerald-600"><i class="fa-solid fa-shield-check mt-0.5"></i><span>Nenhum sinal de risco.</span></li>';
}
function notas() { $('notas').innerHTML = u.notas.length ? u.notas.map(n => '<li class="rounded-xl bg-slate-50 p-3"><p class="text-slate-700">' + esc(n.txt) + '</p><p class="text-[11px] text-slate-400 mt-1">' + esc(n.autor || ADM.nome) + ' · ' + dataHora(n.t) + '</p></li>').join('') : '<li class="text-xs text-slate-400">Nenhuma nota ainda.</li>'; }
$('form-nota').onsubmit = e => {
  e.preventDefault(); const t = $('nota').value.trim(); if (!t) return;
  osApi('admin/usuarios/' + u.id + '/notas', { texto: t }).then(n => { u.notas.unshift(n); $('nota').value = ''; notas(); }, er => aviso(er.message));
};
function hist() {
  const ev = LOG.filter(x => x.alvoId === u.id || (x.alvoId == null && x.alvo === u.nome)).map(x => ({ t: x.t, txt: x.acao + (x.extra ? ' · ' + x.extra : ''), ator: x.ator }));
  ev.push({ t: Date.now() - u.criado * 864e5, txt: 'Cadastro criado na plataforma', ator: 'Sistema' });
  $('hist').innerHTML = ev.sort((a, b) => b.t - a.t).map(x => '<li class="flex gap-3"><span class="w-2.5 h-2.5 rounded-full bg-violet-400 mt-1.5 shrink-0"></span><div><p class="text-slate-700">' + esc(x.txt) + '</p><p class="text-[11px] text-slate-400">' + esc(x.ator) + ' · ' + dataHora(x.t) + '</p></div></li>').join('');
}
function tudo() { cabecalho(); dados(); docs(); acoes(); metricas(); sinais(); notas(); hist(); }

if (!u) { $('conteudo').classList.add('hidden'); $('nao-achou').classList.remove('hidden'); } else tudo();
/* A lista é relida a cada 15 s: atualiza este utilizador com os dados novos. */
aoMudarAdmin = () => { const novo = u && porId(u.id); if (novo && novo !== u) { Object.assign(u, novo); tudo(); } else if (u) tudo(); };
