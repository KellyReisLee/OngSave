/* Gerir Utilizadores: abas por tipo, filtros, busca, ordenação, paginação, ações em lote e CSV. */
const POR_PAGINA = 8;
const q = new URLSearchParams(location.search);
let tipo = PLURAL[q.get('tipo')] ? q.get('tipo') : 'empresa', filtro = ST[q.get('status')] ? q.get('status') : 'todos', busca = '', ordem = 'recentes', pag = 1, linhasAtuais = [];
const sel = new Set();
const CHIPS = [['todos', 'Todos'], ['pendente', 'Pendentes'], ['ativo', 'Ativos'], ['suspenso', 'Suspensos'], ['bloqueado', 'Bloqueados'], ['rejeitado', 'Rejeitados']];
const elegivel = u => u.status === 'pendente' && u.docs.every(d => d.status === 'ok');
const detalhe = u => u.tipo === 'empresa' ? esc((PLAT.planos[u.plano] || { nome: u.plano || '-' }).nome) : u.tipo === 'motorista' ? esc(u.veiculo) + ' · ' + esc(u.placa) : fmt(u.familias) + ' famílias · ' + fmt(u.capKg) + ' kg/dia';
const pesoRisco = u => ({ alto: 3, medio: 2, baixo: 1 }[risco(u).nivel]);

function abas() {
  $('abas').innerHTML = Object.keys(PLURAL).map(t => {
    const n = USUARIOS.filter(u => u.tipo === t).length, p = USUARIOS.filter(u => u.tipo === t && u.status === 'pendente').length, on = t === tipo;
    return '<button data-t="' + t + '" role="tab" aria-selected="' + on + '" class="px-5 py-3 rounded-2xl text-sm font-bold border transition-colors ' + (on ? 'bg-violet-600 border-violet-600 text-white shadow-md shadow-violet-600/20' : 'bg-white border-slate-200 text-slate-600 hover:border-violet-300') + '">' +
      '<i class="fa-solid ' + TIPOS[t][1] + ' mr-2"></i>' + PLURAL[t] + ' <span class="opacity-70">' + n + '</span>' + (p ? ' <span class="ml-1 px-1.5 py-0.5 rounded-full text-[10px] ' + (on ? 'bg-white/25' : 'bg-amber-100 text-amber-700') + '">' + p + ' novo(s)</span>' : '') + '</button>';
  }).join('');
  $('titulo-lista').textContent = PLURAL[tipo];
  $('col-det').textContent = tipo === 'empresa' ? 'Plano' : tipo === 'motorista' ? 'Veículo' : 'Capacidade';
}
function lista() {
  const t = busca.trim().toLowerCase();
  let l = USUARIOS.filter(u => u.tipo === tipo && (filtro === 'todos' || u.status === filtro) && (!t || (u.nome + ' ' + u.doc + ' ' + u.email).toLowerCase().includes(t)));
  l.sort((a, b) => ordem === 'nome' ? a.nome.localeCompare(b.nome) : ordem === 'risco' ? pesoRisco(b) - pesoRisco(a) || a.criado - b.criado : a.criado - b.criado);
  linhasAtuais = l;
  const paginas = Math.max(1, Math.ceil(l.length / POR_PAGINA)); pag = Math.min(pag, paginas);
  return l.slice((pag - 1) * POR_PAGINA, pag * POR_PAGINA);
}
const botoes = u => {
  const b = (rot, a, cls) => '<button data-a="' + a + '" data-id="' + u.id + '" class="' + (cls || 'btn-sec') + ' !py-1.5 !px-3 text-xs">' + rot + '</button>';
  if (u.status === 'pendente') return (elegivel(u) ? b('Aprovar', 'aprovar', 'btn-lar') : '') + b('Rejeitar', 'rejeitar');
  if (u.status === 'ativo') return b('Suspender', 'suspender');
  if (u.status === 'suspenso' || u.status === 'bloqueado') return b('Reativar', 'reativar', 'btn-lar');
  return '';
};
function desenhar() {
  abas();
  $('chips').innerHTML = CHIPS.map(c => '<button data-f="' + c[0] + '" class="chip px-3 py-1.5 rounded-full text-xs font-bold border border-slate-200 text-slate-600 hover:bg-slate-50" aria-pressed="' + (filtro === c[0]) + '">' + c[1] + ' <span class="opacity-70">' +
    USUARIOS.filter(u => u.tipo === tipo && (c[0] === 'todos' || u.status === c[0])).length + '</span></button>').join('');
  const pagina = lista();
  $('linhas').innerHTML = pagina.map(u => {
    const r = risco(u);
    return '<tr class="hover:bg-slate-50/50"><td class="py-4 px-4"><input type="checkbox" data-s="' + u.id + '" ' + (sel.has(u.id) ? 'checked' : '') + ' class="accent-violet-600 w-4 h-4" aria-label="Selecionar ' + esc(u.nome) + '"></td>' +
      '<td class="py-4 px-4"><a href="detalhe-utilizador?id=' + u.id + '" class="font-semibold text-slate-900 hover:text-violet-600">' + esc(u.nome) + '</a><p class="text-[11px] text-slate-400">' + esc(u.doc) + '</p></td>' +
      '<td class="py-4 px-4 text-slate-600"><p>' + esc(u.email) + '</p><p class="text-[11px] text-slate-400">' + esc(u.tel) + ' · ' + esc(u.cidade) + '</p></td>' +
      '<td class="py-4 px-4 text-slate-600">' + detalhe(u) + '</td><td class="py-4 px-4">' + pill(ST[u.status]) + '</td><td class="py-4 px-4">' + pillRisco(r.nivel) + '</td>' +
      '<td class="py-4 px-4 text-slate-500 text-xs">' + quando(u.criado) + '</td>' +
      '<td class="py-4 px-4 text-right"><div class="flex justify-end gap-2 flex-wrap"><a href="detalhe-utilizador?id=' + u.id + '" class="p-2 bg-slate-100 hover:bg-violet-50 hover:text-violet-600 rounded-xl text-slate-600 text-xs" aria-label="Abrir ' + esc(u.nome) + '"><i class="fa-solid fa-eye"></i></a>' + botoes(u) + '</div></td></tr>';
  }).join('');
  $('vazio').classList.toggle('hidden', linhasAtuais.length > 0);
  const paginas = Math.max(1, Math.ceil(linhasAtuais.length / POR_PAGINA));
  $('pag-info').textContent = linhasAtuais.length ? 'Página ' + pag + ' de ' + paginas + ' · ' + linhasAtuais.length + ' utilizador(es)' : '';
  $('pag-ant').disabled = pag <= 1; $('pag-prox').disabled = pag >= paginas;
  $('todos').checked = pagina.length > 0 && pagina.every(u => sel.has(u.id));
  $('lote').classList.toggle('hidden', sel.size === 0);
  $('lote-n').textContent = sel.size + ' selecionado(s)';
}

/* Interações */
$('abas').onclick = e => { const b = e.target.closest('button[data-t]'); if (b) { tipo = b.dataset.t; pag = 1; sel.clear(); desenhar(); } };
$('chips').onclick = e => { const b = e.target.closest('button[data-f]'); if (b) { filtro = b.dataset.f; pag = 1; desenhar(); } };
$('busca').oninput = e => { busca = e.target.value; pag = 1; desenhar(); };
$('ordem').onchange = e => { ordem = e.target.value; desenhar(); };
$('pag-ant').onclick = () => { pag--; desenhar(); };
$('pag-prox').onclick = () => { pag++; desenhar(); };
$('todos').onchange = e => { lista().forEach(u => { e.target.checked ? sel.add(u.id) : sel.delete(u.id); }); desenhar(); };
$('linhas').addEventListener('change', e => { const c = e.target.closest('input[data-s]'); if (c) { c.checked ? sel.add(+c.dataset.s) : sel.delete(+c.dataset.s); desenhar(); } });
$('lote-limpar').onclick = () => { sel.clear(); desenhar(); };

$('linhas').addEventListener('click', async e => {
  const b = e.target.closest('button[data-a]'); if (!b) return;
  const u = porId(b.dataset.id); if (!u) return;
  const novo = { aprovar: 'ativo', rejeitar: 'rejeitado', suspender: 'suspenso', reativar: 'ativo' }[b.dataset.a];
  if (novo === 'ativo' && b.dataset.a === 'reativar' && !confirm('Reativar a conta de ' + u.nome + '?')) return;
  if (await acao(u, novo)) desenhar();
});
$('lote-aprovar').onclick = async () => {
  const alvo = [...sel].map(porId).filter(Boolean), ok = alvo.filter(elegivel);
  let feitos = 0;
  for (const u of ok) { try { await mudarStatus(u, 'ativo'); feitos++; } catch (e) { aviso(u.nome + ': ' + e.message); } }
  aviso(feitos + ' cadastro(s) aprovado(s).' + (alvo.length - ok.length ? ' ' + (alvo.length - ok.length) + ' ficaram de fora (não pendentes ou com documento a revisar).' : ''));
  sel.clear(); desenhar();
};
$('lote-suspender').onclick = async () => {
  const alvo = [...sel].map(porId).filter(u => u && u.status === 'ativo');
  if (!alvo.length) return aviso('Selecione ao menos uma conta ativa para suspender.');
  const r = await pedirMotivo({ titulo: 'Suspender ' + alvo.length + ' conta(s)', msg: 'A mesma justificativa vale para todas e fica na auditoria de cada uma.', motivos: MOTIVOS.suspenso, obrigatorio: true, botao: 'Suspender' });
  if (!r) return;
  let feitos = 0;
  for (const u of alvo) { try { await mudarStatus(u, 'suspenso', r); feitos++; } catch (e) { aviso(u.nome + ': ' + e.message); } }
  aviso(feitos + ' conta(s) suspensa(s).'); sel.clear(); desenhar();
};
$('btn-csv').onclick = () => baixar('utilizadores-' + tipo + '.csv', csv([['Nome', 'Documento', 'E-mail', 'Telefone', 'Situação', 'Risco', 'Entregas', 'Cadastro'],
  ...linhasAtuais.map(u => [u.nome, u.doc, u.email, u.tel, ST[u.status][0], risco(u).nivel, u.st.entregas, quando(u.criado)])]), 'text/csv;charset=utf-8');

$('ordem').value = ordem; desenhar();
aoMudarAdmin = () => desenhar();
