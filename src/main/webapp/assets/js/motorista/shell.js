/* Menu lateral, topo e utilitários compartilhados pelas páginas do motorista.
   Cada página traz <body data-pagina="..." data-titulo="..."> e o conteúdo dentro de <section id="pagina">.
   Os dados do motorista vêm do servidor (OS_ESTADO.motorista); só a preferência Online/Offline fica no aparelho. */
const $ = id => document.getElementById(id);
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const brl = v => Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const quando = dias => dias === 0 ? 'Hoje' : new Date(Date.now() - dias * 864e5).toLocaleDateString('pt-BR');
const MOT = OS_ESTADO.motorista || { nome: '', veiculo: 'Carro Económico', consentimento: null };
const VEICULO = MOT.veiculo;
const online = () => { try { return localStorage.getItem('ongsave.online') !== '0'; } catch (e) { return true; } };

function aviso(msg, tipo) {
  const t = $('toast');
  t.textContent = msg;
  t.className = 'toast ' + (tipo === 'erro' ? 'bg-red-600' : 'bg-slate-800');
  clearTimeout(aviso.t);
  aviso.t = setTimeout(() => t.classList.add('hidden'), 4000);
}

/* Consentimento de localização (LGPD): pedido antes da primeira entrega aceita e revogável no perfil. */
function garantirConsentimento() {
  if (MOT.consentimento) return Promise.resolve(true);
  return new Promise(res => {
    const v = document.createElement('div');
    v.className = 'fixed inset-0 z-[70] grid place-items-center p-4 bg-slate-900/50';
    v.innerHTML = '<div role="dialog" aria-modal="true" aria-labelledby="cs-t" class="bg-white rounded-2xl w-full max-w-md p-6 space-y-4"><h3 id="cs-t" class="text-lg font-extrabold">Compartilhar sua localização</h3>' +
      '<ul class="text-sm text-slate-600 space-y-2 list-disc pl-5"><li>Usamos sua posição para mostrar entregas no seu raio e, <b>só durante uma entrega ativa</b>, para a empresa e a ONG acompanharem o trajeto.</li>' +
      '<li>O rastreio para quando a entrega termina.</li><li>O histórico de posições é guardado por prazo limitado, para segurança e auditoria.</li><li>Você pode revogar o consentimento no seu perfil, mas não poderá aceitar entregas sem ele.</li></ul>' +
      '<div class="flex justify-end gap-3"><button id="cs-n" class="btn-verde !bg-slate-200 !text-slate-700">Agora não</button><button id="cs-s" class="btn-verde">Concordo</button></div></div>';
    document.body.appendChild(v);
    const fim = ok => {
      v.remove();
      if (!ok) return res(false);
      osApi('motorista/consentimento', { conceder: true })
        .then(r => { MOT.consentimento = r.consentimento || new Date().toISOString(); res(true); })
        .catch(e => { aviso(e.message, 'erro'); res(false); });
    };
    v.querySelector('#cs-n').onclick = () => fim(false); v.querySelector('#cs-s').onclick = () => fim(true); v.querySelector('#cs-s').focus();
  });
}

const NOME_MOTORISTA = MOT.nome || document.body.dataset.usuario || 'Motorista';

(function montarShell() {
  const pag = document.body.dataset.pagina, titulo = document.body.dataset.titulo;
  const itens = [
    ['dashboard', 'fa-chart-pie', 'Visão Geral', 'geral'],
    ['entregas', 'fa-map-location-dot', 'Entregas', 'entregas'],
    ['rota', 'fa-route', 'Rota Ativa', 'rota'],
    ['historico', 'fa-clock-rotate-left', 'Histórico', 'historico'],
    ['carteira', 'fa-wallet', 'Carteira', 'carteira'],
    ['perfil', 'fa-id-card', 'Perfil e Veículo', 'perfil']
  ];
  const menu = itens.map(i =>
    `<a class="nav-item ${i[3] === pag ? 'ativo' : ''}" href="${i[0]}"><i class="fa-solid ${i[1]} w-4"></i> ${i[2]}</a>`).join('');

  document.body.insertAdjacentHTML('afterbegin', `
  <div class="flex flex-col md:flex-row min-h-screen">
    <nav class="bg-slate-900 text-white md:w-60 md:h-screen md:sticky md:top-0 flex md:flex-col gap-1 p-3 overflow-x-auto md:overflow-visible shrink-0">
      <div class="hidden md:flex items-center gap-3 px-2 py-3 mb-3 border-b border-slate-800">
        <span class="grid place-items-center w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-700"><i class="fa-solid fa-leaf"></i></span>
        <div><p class="font-extrabold leading-none">Ong<span class="text-emerald-400">Save</span></p><p class="text-[10px] text-slate-400 mt-1">PAINEL MOTORISTA</p></div>
      </div>
      ${menu}
      <a class="nav-item md:mt-auto !text-red-400" href="../logout"><i class="fa-solid fa-right-from-bracket w-4"></i> Encerrar Sessão</a>
    </nav>
    <div class="flex-1 min-w-0">
      <header class="bg-white border-b border-slate-200 h-16 px-4 md:px-6 flex items-center gap-3 sticky top-0 z-[1000]">
        <h1 class="font-extrabold text-lg">${titulo}</h1>
        <button id="btn-online" class="online on"><span class="dot"></span><span id="online-txt">Online</span></button>
        <span class="ml-auto hidden sm:block text-xs text-slate-500"><i class="fa-solid fa-location-crosshairs"></i> <span id="gps-txt">—</span></span>
        <div class="text-right leading-tight hidden sm:block">
          <p class="text-sm font-bold">${esc(NOME_MOTORISTA)}</p>
          <p class="text-xs text-emerald-600 font-semibold">${esc(VEICULO)} · Conta aprovada</p>
        </div>
        <span class="grid place-items-center w-9 h-9 rounded-full bg-emerald-100 text-emerald-700 font-bold text-sm">${esc(NOME_MOTORISTA.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase())}</span>
      </header>
      <main id="conteudo" class="${document.body.dataset.largo ? '' : 'p-4 md:p-6 max-w-5xl mx-auto'}"></main>
    </div>
  </div>
  <div id="toast" class="toast hidden" role="status"></div>`);
  $('conteudo').appendChild($('pagina'));

  const pintar = () => {
    $('btn-online').className = 'online ' + (online() ? 'on' : 'off');
    $('online-txt').textContent = online() ? 'Online' : 'Offline';
  };
  $('btn-online').onclick = () => {
    try { localStorage.setItem('ongsave.online', online() ? '0' : '1'); } catch (e) { /* modo privado */ }
    pintar();
    window.dispatchEvent(new Event('online-mudou')); // as páginas recarregam as entregas
  };
  pintar();
})();
document.querySelectorAll('.raio-km').forEach(e => { e.textContent = PLAT.raioKm; }); // raio definido pelo administrador

/* Notificações do servidor (aceite da ONG, retirada registada, token gerado, frete liberado...) aparecem como aviso. */
let vistasNotif = new Set((OS_ESTADO.notifs || []).map(n => n.id));
osAcompanhar('motorista', e => {
  if (e.motorista) Object.assign(MOT, e.motorista);
  const novas = (e.notifs || []).filter(n => !vistasNotif.has(n.id));
  novas.forEach(n => vistasNotif.add(n.id));
  if (novas.length) { aviso(novas[0].t); osMarcarLidas(); }
  if (typeof aoEstado === 'function') aoEstado(e);
}, 15000);
