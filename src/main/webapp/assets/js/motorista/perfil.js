/* OngSave · motorista/perfil: dados pessoais, veículo, CNH, morada, consentimento (LGPD) e contestação. */
const DICA = {
  'Carro Económico': 'Você verá entregas que caibam em um carro econômico.',
  'Camionete': 'Você verá entregas para carro econômico e camionete.',
  'Van': 'Você verá entregas para carro econômico, camionete e van.',
  'Caminhão': 'Você verá todas as entregas, inclusive as de caminhão.'
};
const CAMPOS_M = { nome: 'm-nome', email: 'm-email', tel: 'm-tel', veiculo: 'veiculo', placa: 'm-placa', modelo: 'm-modelo', cnh: 'm-cnh', cat: 'm-cat',
  cnhValidade: 'cnh-val', cep: 'm-cep', rua: 'm-rua', num: 'm-num', bairro: 'm-bairro', cidade: 'm-cidade', uf: 'm-uf' };
const dica = () => { $('dica-veiculo').textContent = DICA[$('veiculo').value]; };

function preencher() {
  Object.entries(CAMPOS_M).forEach(([k, id]) => { $(id).value = MOT[k] ?? ''; });
  $('m-cpf').value = MOT.cpf || '';
  dica(); statusCnh();
}
function statusCnh() {
  const d = Math.ceil((new Date($('cnh-val').value) - Date.now()) / 864e5), b = $('cnh-status');
  if (isNaN(d)) { b.textContent = ''; return; }
  b.textContent = d < 0 ? 'Vencida' : 'Vence em ' + d + ' dias';
  b.className = 'badge ml-1 ' + (d < 0 ? 'bg-red-100 text-red-700' : d < 60 ? 'bg-amber-100 text-amber-700' : 'bg-emerald-100 text-emerald-700');
}
$('veiculo').onchange = dica;
$('cnh-val').onchange = statusCnh;

/* Máscaras e busca de CEP */
const mascTel = v => {
  v = v.replace(/\D/g, '').slice(0, 11);
  return v.length > 10 ? v.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3')
    : v.length > 6 ? v.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3')
    : v.length > 2 ? v.replace(/(\d{2})(\d*)/, '($1) $2') : v;
};
$('m-tel').addEventListener('input', () => { $('m-tel').value = mascTel($('m-tel').value); });
$('m-placa').addEventListener('input', () => { $('m-placa').value = $('m-placa').value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 7); });
$('m-cnh').addEventListener('input', () => { $('m-cnh').value = $('m-cnh').value.replace(/\D/g, '').slice(0, 11); });
$('m-uf').addEventListener('input', () => { $('m-uf').value = $('m-uf').value.replace(/[^a-zA-Z]/g, '').toUpperCase(); });
$('m-cep').addEventListener('input', async () => {
  let v = $('m-cep').value.replace(/\D/g, '').slice(0, 8);
  $('m-cep').value = v.length > 5 ? v.slice(0, 5) + '-' + v.slice(5) : v;
  if (v.length !== 8) { $('cep-st').textContent = ''; return; }
  $('cep-st').textContent = 'Buscando endereço...';
  try {
    const r = await (await fetch('https://viacep.com.br/ws/' + v + '/json/')).json();
    if (r.erro) { $('cep-st').textContent = 'CEP não encontrado.'; return; }
    $('m-rua').value = r.logradouro || $('m-rua').value; $('m-bairro').value = r.bairro || '';
    $('m-cidade').value = r.localidade || ''; $('m-uf').value = r.uf || '';
    $('cep-st').textContent = 'Endereço preenchido. Confira o número.';
  } catch (e) { $('cep-st').textContent = 'Não foi possível consultar o CEP agora.'; }
});

$('form-perfil').onsubmit = e => {
  e.preventDefault();
  const d = Object.fromEntries(Object.entries(CAMPOS_M).map(([k, id]) => [k, $(id).value.trim()]));
  $('btn-salvar').disabled = true;
  osApi('motorista/perfil', d)
    .then(novo => {
      Object.assign(MOT, novo); preencher();
      aviso('Alterações guardadas.' + (novo.veiculo !== VEICULO ? ' Recarregue as páginas para filtrar as entregas pelo novo veículo.' : ''));
    })
    .catch(er => aviso(er.message, 'erro'))
    .finally(() => { $('btn-salvar').disabled = false; });
};

const NOMES_V = { carro: 'Carro', camionete: 'Camionete', van: 'Van', caminhao: 'Caminhão' };
$('tabela-frete').textContent = Object.entries(PLAT.fretes).map(([k, v]) => NOMES_V[k] + ' R$ ' + v).join(' · ');

function consentimento() {
  const c = MOT.consentimento;
  $('consent-st').textContent = c ? 'Consentimento dado em ' + new Date(c).toLocaleDateString('pt-BR') + '.' : 'Consentimento ainda não dado.';
  $('btn-revogar').disabled = !c;
}
$('btn-revogar').onclick = () => {
  if (!confirm('Revogar o consentimento de localização? Sem ele não poderá aceitar entregas.')) return;
  osApi('motorista/consentimento', { conceder: false })
    .then(() => { MOT.consentimento = null; consentimento(); aviso('Consentimento revogado. Para aceitar entregas, será preciso consentir de novo.'); })
    .catch(er => aviso(er.message, 'erro'));
};
$('btn-contestar').onclick = () => {
  const texto = prompt('Descreva a decisão que quer contestar (lote, data, motivo):', '');
  if (texto === null) return;
  osApi('motorista/contestar', { texto })
    .then(() => aviso('Contestação enviada. A administração responde por escrito.'))
    .catch(er => aviso(er.message, 'erro'));
};

preencher(); consentimento();
