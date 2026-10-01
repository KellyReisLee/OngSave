let selectedType = '';

function selectProfile(type) {
  selectedType = type;

  // Remover estilos ativos de todos os cartões
  document.querySelectorAll('.profile-card').forEach(card => {
    card.classList.remove('border-orange-500', 'border-emerald-500', 'border-blue-500', 'border-purple-500', 'bg-orange-50/20', 'shadow-xl');
    const icon = card.querySelector('.check-icon i');
    icon.className = "fa-regular fa-circle text-xl text-slate-300";
  });

  // Destacar o cartão selecionado
  const selectedCard = document.getElementById(`card-${type}`);
  const activeIcon = selectedCard.querySelector('.check-icon i');
  activeIcon.className = "fa-solid fa-circle-check text-xl";

  const standardFields = document.getElementById('standard-extra-fields');
  const driverFields = document.getElementById('driver-extra-fields');
  const adminFields = document.getElementById('admin-extra-fields'); // Bloco do Admin

  // Ocultar todos os campos extras inicialmente
  if (standardFields) standardFields.classList.add('hidden');
  if (driverFields) driverFields.classList.add('hidden');
  if (adminFields) adminFields.classList.add('hidden');

  if (type === 'empresa') {
    selectedCard.classList.add('border-orange-500', 'shadow-xl');
    activeIcon.classList.add('text-orange-600');
    standardFields.classList.remove('hidden');

    document.getElementById('extra-input').setAttribute('required', 'true');
    toggleDriverRequired(false);

    updateFormConfig({
      subtitle: 'Perfil Selecionado: Empresa Doadora',
      title: 'Dados da Empresa Doadora',
      labelName: 'Razão Social / Nome Fantasia',
      placeholderName: 'Ex: Supermercados Exemplo Ltda',
      docLabel: 'CNPJ',
      docPlaceholder: '00.000.000/0001-00',
      extraLabel: 'Setor de Atuação',
      extraPlaceholder: 'Ex: Varejo / Supermercado / Indústria',
      extraIcon: 'fa-briefcase',
      sectionTitle: 'Detalhes Comerciais'
    });
  } else if (type === 'motorista') {
    selectedCard.classList.add('border-emerald-500', 'shadow-xl');
    activeIcon.classList.add('text-emerald-600');
    driverFields.classList.remove('hidden');

    document.getElementById('extra-input').removeAttribute('required');
    toggleDriverRequired(true);

    updateFormConfig({
      subtitle: 'Perfil Selecionado: Motorista Parceiro',
      title: 'Dados do Motorista e Veículo',
      labelName: 'Nome Completo',
      placeholderName: 'Ex: João da Silva',
      docLabel: 'CPF',
      docPlaceholder: '000.000.000-00',
      sectionTitle: 'Credenciais de Condução e Veículo'
    });
  } else if (type === 'ong') {
    selectedCard.classList.add('border-blue-500', 'shadow-xl');
    activeIcon.classList.add('text-blue-600');
    standardFields.classList.remove('hidden');

    document.getElementById('extra-input').setAttribute('required', 'true');
    toggleDriverRequired(false);

    updateFormConfig({
      subtitle: 'Perfil Selecionado: Instituição (ONG)',
      title: 'Dados da Instituição Social',
      labelName: 'Nome da Instituição',
      placeholderName: 'Ex: Associação Comunitária Esperança',
      docLabel: 'CNPJ da ONG',
      docPlaceholder: '00.000.000/0001-00',
      extraLabel: 'Público Atendido / Nº Famílias',
      extraPlaceholder: 'Ex: 150 famílias carentes',
      extraIcon: 'fa-hands-holding-child',
      sectionTitle: 'Detalhes da Comunidade'
    });
  } else if (type === 'admin') {
    selectedCard.classList.add('border-purple-500', 'shadow-xl');
    activeIcon.classList.add('text-purple-600');
    if (adminFields) adminFields.classList.remove('hidden');

    document.getElementById('extra-input').removeAttribute('required');
    toggleDriverRequired(false);

    updateFormConfig({
      subtitle: 'Perfil Selecionado: Administrador',
      title: 'Dados do Administrador Interno',
      labelName: 'Nome Completo',
      placeholderName: 'Ex: Carlos Alberto',
      docLabel: 'CPF',
      docPlaceholder: '000.000.000-00',
      sectionTitle: 'Governança e Nível de Acesso'
    });
  }

  const formContainer = document.getElementById('form-container');
  formContainer.classList.remove('hidden');
  formContainer.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function toggleDriverRequired(isEnable) {
  const cnh = document.getElementById('input-cnh');
  const categoria = document.getElementById('input-categoria');
  const placa = document.getElementById('input-placa');
  const phone = document.getElementById('driver-phone');

  if (cnh) isEnable ? cnh.setAttribute('required', 'true') : cnh.removeAttribute('required');
  if (categoria) isEnable ? categoria.setAttribute('required', 'true') : categoria.removeAttribute('required');
  if (placa) isEnable ? placa.setAttribute('required', 'true') : placa.removeAttribute('required');
  if (phone) isEnable ? phone.setAttribute('required', 'true') : phone.removeAttribute('required');
}

function updateFormConfig(config) {
  document.getElementById('form-subtitle').innerText = config.subtitle;
  document.getElementById('form-title').innerText = config.title;
  document.getElementById('label-name').innerText = config.labelName;
  document.getElementById('input-name').placeholder = config.placeholderName;
  document.getElementById('dynamic-label').innerText = config.docLabel;
  document.getElementById('dynamic-input').placeholder = config.docPlaceholder;
  document.getElementById('section-extra-text').innerText = config.sectionTitle;

  if (config.extraLabel) {
    const extraLabelElem = document.getElementById('extra-label');
    const extraInputElem = document.getElementById('extra-input');
    const extraIconElem = document.getElementById('extra-icon-elem');

    if (extraLabelElem) extraLabelElem.innerText = config.extraLabel;
    if (extraInputElem) extraInputElem.placeholder = config.extraPlaceholder;
    if (extraIconElem && config.extraIcon) extraIconElem.className = `fa-solid ${config.extraIcon}`;
  }
}

function resetSelection() {
  document.getElementById('form-container').classList.add('hidden');
  document.querySelectorAll('.profile-card').forEach(card => {
    card.classList.remove('border-orange-500', 'border-emerald-500', 'border-blue-500', 'border-purple-500', 'shadow-xl');
    card.querySelector('.check-icon i').className = "fa-regular fa-circle text-xl text-slate-300";
  });
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function handleRegistration(event) {
  // Agora o envio é real: o FrontController (POST /cadastro) valida e cria a conta.
  if (!selectedType) {
    event.preventDefault();
    alert('Escolha um perfil antes de continuar.');
    return;
  }
  document.getElementById('input-perfil').value = selectedType;
}

// Se o servidor devolveu a página com erro, reabre o formulário no perfil escolhido.
document.addEventListener('DOMContentLoaded', () => {
  const perfil = document.getElementById('input-perfil')?.value;
  if (perfil && document.getElementById(`card-${perfil}`)) selectProfile(perfil);
});
