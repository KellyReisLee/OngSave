/* Registar Novo Lote: validação, sugestão de veículo, resumo ao vivo, foto, rascunho e publicação. */
const VEICS = { carro: ['Carro Utilitário', 80], van: ['Van', 300], cam: ['Caminhão de Carga', Infinity] };
const CAMPOS = ['tipo', 'kg', 'vol', 'cons', 'validade', 'j1', 'j2', 'veic', 'ong', 'obs'];
const COM_TEMP = () => ['ref', 'cong'].includes($('cons').value);
let manual = false, foto = null;
const ONGS_LISTA = OS_ESTADO.ongsLista || [];

const vSug = kg => kg > 300 ? 'cam' : kg > 80 ? 'van' : 'carro';
const local = ms => new Date(ms - new Date().getTimezoneOffset() * 6e4).toISOString().slice(0, 16);

function erro(id, msg) {
  const p = document.querySelector('[data-e="' + id + '"]'); if (p) p.textContent = msg || '';
  const c = $(id); if (c) c.classList.toggle('invalido', !!msg);
}

/* Início: opções, morada do perfil e valores padrão */
$('ong').innerHTML = '<option value="auto">Automática (mais próxima e compatível)</option>' +
  ONGS_LISTA.slice().sort((a, b) => a.km - b.km).map(o => '<option value="' + o.id + '">' + esc(o.nome) + ' · ' + fmt(o.km, 1) + ' km</option>').join('');
const pe = perfilEmpresa();
$('morada').value = pe.rua ? pe.rua + ', ' + pe.num + (pe.compl ? ' - ' + pe.compl : '') + ' - ' + pe.cidade + '/' + pe.uf : 'Complete o endereço em Subscrição & Perfil';
$('validade').min = local(Date.now());
$('validade').value = local(Date.now() + 6 * 36e5);

const rasc = ler('rascunho', null);
if (rasc) {
  CAMPOS.forEach(k => { if (rasc[k] && !(k === 'validade' && rasc[k] < $('validade').min) && !(k === 'ong' && ![...$('ong').options].some(o => o.value === rasc[k]))) $(k).value = rasc[k]; });
  if (rasc.kg) aviso('Rascunho restaurado.');
}

/* Resumo e regras ao vivo */
function atualizar() {
  const kg = +$('kg').value || 0, sug = vSug(kg);
  if (!manual && kg) $('veic').value = sug;
  const cap = VEICS[$('veic').value][1], dica = $('dica-veic');
  if (!kg) { dica.textContent = ''; }
  else if (kg > cap) { dica.textContent = 'Este veículo comporta até ' + cap + ' kg. Escolha um maior.'; dica.className = 'text-xs mt-1.5 text-red-600 font-semibold'; }
  else {
    dica.textContent = 'Sugestão para ' + fmt(kg) + ' kg: ' + VEICS[sug][0] + '.' + (COM_TEMP() ? ' Carga com controle de temperatura: avise nas instruções se exige veículo refrigerado.' : '');
    dica.className = 'text-xs mt-1.5 text-slate-500';
  }
  const h = $('validade').value ? (new Date($('validade').value) - Date.now()) / 36e5 : null;
  $('r-prazo').textContent = h === null ? '-' : h <= 0 ? 'vencido' : 'em ' + hm(h) + (h < 2 ? ' (prazo curto)' : '');
  $('r-prazo').className = 'font-semibold text-right ' + (h !== null && h < 2 ? 'text-amber-600' : '');
  $('r-tipo').textContent = $('tipo').value || '-';
  $('r-kg').textContent = kg ? fmt(kg) + ' kg' : '-';
  $('r-veic').textContent = VEICS[$('veic').value][0] + (COM_TEMP() ? ' (refrigerado)' : '');
  $('r-ong').textContent = $('ong').value === 'auto' ? 'Automática' : $('ong').selectedOptions[0].textContent;
  $('r-ref').textContent = fmt(kg * REFEICOES_POR_KG);
  $('r-co2').textContent = fmt(kg * CO2_POR_KG / 1000, 2) + ' t';
  $('obs-n').textContent = $('obs').value.length;
  const u = usoPlano(), num = u.uso + 1;
  $('r-plano').innerHTML = num > u.franquia
    ? '<b class="text-red-600">Lote acima da franquia:</b> será o ' + num + 'º do mês (franquia de ' + u.franquia + '). Taxa extra de R$ ' + fmt(u.taxa) + '.'
    : 'Lote ' + num + ' de ' + u.franquia + ' da franquia do plano neste mês.';
  guardar('rascunho', Object.fromEntries(CAMPOS.map(k => [k, $(k).value])));
}
$('veic').addEventListener('change', () => { manual = true; });
$('form-lote').addEventListener('input', atualizar);
$('form-lote').addEventListener('change', atualizar);

$('motoristas').innerHTML = '<i class="fa-solid fa-user-clock mr-1"></i> <b>' + fmt(OS_ESTADO.motoristasAtivos || 0) + '</b> motorista(s) ativo(s) na plataforma nos últimos 7 dias. A ONG recebe a proposta e, ao aceitar, os motoristas do raio de ' + PLAT.raioKm + ' km são avisados.';

/* Foto: clique, arrastar e soltar, validação de tipo e tamanho */
function pegarFoto(f) {
  erro('foto', '');
  if (!f) return;
  if (!/^image\/(png|jpeg)$/.test(f.type)) return erro('foto', 'Use uma imagem PNG ou JPG.');
  if (f.size > 5 * 1024 * 1024) return erro('foto', 'A foto passa de 5 MB.');
  foto = f;
  $('prev').src = URL.createObjectURL(f);
  $('prev').classList.remove('hidden'); $('drop-vazio').classList.add('hidden');
}
$('foto').onchange = () => pegarFoto($('foto').files[0]);
['dragover', 'drop'].forEach(ev => $('drop').addEventListener(ev, e => {
  e.preventDefault();
  if (ev === 'drop') pegarFoto(e.dataTransfer.files[0]);
}));

/* Publicação */
$('form-lote').onsubmit = ev => {
  ev.preventDefault();
  const kg = +$('kg').value, cap = VEICS[$('veic').value][1], v = $('validade').value;
  const e = {
    tipo: !$('tipo').value && 'Selecione a categoria.',
    kg: !(kg > 0) ? 'Informe o peso.' : kg > cap && 'O veículo escolhido não comporta este peso.',
    vol: !(+$('vol').value > 0) && 'Informe a quantidade de volumes.',
    cons: !$('cons').value && 'Selecione o requisito de conservação.',
    validade: (!v || new Date(v) <= new Date()) && 'Informe uma validade futura.',
    janela: $('j1').value && $('j2').value && $('j1').value >= $('j2').value && 'O início deve ser antes do fim.',
    foto: !foto && 'A foto do lote é obrigatória.',
    termo: !$('termo').checked && 'Aceite o termo de responsabilidade para publicar.'
  };
  Object.keys(e).forEach(k => erro(k, e[k] || ''));
  const primeiro = Object.keys(e).find(k => e[k]);
  if (primeiro) { document.querySelector('[data-e="' + primeiro + '"]').scrollIntoView({ behavior: 'smooth', block: 'center' }); return; }

  const uso = usoPlano();
  if (uso.uso + 1 > uso.franquia && !confirm('Este lote ficará acima da franquia do plano e terá taxa extra de R$ ' + fmt(uso.taxa) + '. Publicar mesmo assim?')) return;
  const fd = new FormData();
  CAMPOS.forEach(k => fd.append(k, $(k).value));
  fd.set('validade', new Date(v).toISOString());
  fd.append('termo', $('termo').checked ? 'true' : 'false');
  fd.append('foto', foto, foto.name);
  $('btn-pub').disabled = true;
  $('btn-pub').innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-1"></i> A publicar...';
  osApi('empresa/lotes', fd).then(r => {
    guardar('rascunho', null);
    $('btn-pub').innerHTML = '<i class="fa-solid fa-check mr-1"></i> Lote publicado';
    aviso('Lote #' + r.id + ' publicado. ' + (r.ongId ? 'A ONG recebeu a proposta.' : 'Nenhuma ONG compatível disponível agora; a equipa OngSave foi avisada.'));
    window.onbeforeunload = null;
    setTimeout(() => { location.href = 'dashboard'; }, 1200);
  }).catch(e => {
    $('btn-pub').disabled = false;
    $('btn-pub').innerHTML = '<i class="fa-solid fa-paper-plane mr-1"></i> Publicar lote';
    aviso(e.message);
  });
};

atualizar();
