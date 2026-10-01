/* Página Rota Ativa: coleta, transporte, token e foto */
let ativa = null, fase = 'livre', ultimoEnvio = 0, wake = null, tracoId = 0, primeira = true;
const map = L.map('map').setView([eu.lat, eu.lon], 13);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(map);
const meu = L.marker([eu.lat, eu.lon], {
  icon: L.divIcon({ className: '', html: '<div class="eu"></div>', iconSize: [16, 16] }), zIndexOffset: 1000
}).addTo(map);
const pin = cor => L.divIcon({ className: '', html: '<div class="pin" style="background:' + cor + '"></div>', iconSize: [22, 22] });
const camRota = L.layerGroup().addTo(map);
const trilha = L.polyline([], { color: '#111827', weight: 3, dashArray: '2 6' }).addTo(map);

/* ---------- Rota ativa: coletar, transportar, validar ---------- */
async function iniciarRota(o, f) {
  ativa = o; fase = f;
  trilha.setLatLngs([]);
  $('view-rota').classList.remove('hidden');
  $('rota-empresa').textContent = o.empresa;
  $('rota-resumo').textContent = 'Destino: ' + o.ong + ' · ' + o.pesoKg + ' kg de ' + o.produto + ' · frete ' + brl(o.frete);
  try { wake = await navigator.wakeLock?.request('screen'); } catch (e) { /* opcional */ }
  mostrarFase();
}

async function tracar(de, para) {
  try {
    const u = 'https://router.project-osrm.org/route/v1/driving/' + de.lon + ',' + de.lat + ';' + para.lon + ',' + para.lat + '?overview=full&geometries=geojson';
    const j = await (await fetch(u)).json();
    return j.routes[0].geometry.coordinates.map(c => [c[1], c[0]]);
  } catch (e) {
    return [[de.lat, de.lon], [para.lat, para.lon]]; // sem serviço de rotas: linha reta
  }
}

async function mostrarFase() {
  const nomes = ['coleta', 'transito', 'fim'], idx = nomes.indexOf(fase);
  nomes.forEach(n => $('p-' + n).classList.toggle('hidden', n !== fase));
  $('bloco-alvo').classList.toggle('hidden', fase === 'fim');
  [0, 1, 2].forEach(i => {
    $('s' + (i + 1)).classList.toggle('on', i === idx);
    $('s' + (i + 1)).classList.toggle('done', i < idx);
  });
  camRota.clearLayers();
  if (fase === 'fim') return;

  const coleta = fase === 'coleta', alvo = coleta ? ptColeta(ativa) : ptEntrega(ativa);
  L.marker([alvo.lat, alvo.lon], { icon: pin(coleta ? '#f97316' : '#3b82f6') }).addTo(camRota)
    .bindTooltip(coleta ? ativa.empresa : ativa.ong, { permanent: true, direction: 'top' });
  $('nav-link').href = 'https://www.google.com/maps/dir/?api=1&destination=' + alvo.lat + ',' + alvo.lon;

  const id = ++tracoId;
  const pts = await tracar(eu, alvo);
  if (id !== tracoId) return;
  const linha = L.polyline(pts, { color: '#059669', weight: 5 }).addTo(camRota);
  map.fitBounds(linha.getBounds().pad(0.2));
  atualizarAlvo();
}

function atualizarAlvo() {
  if (!ativa || fase === 'livre' || fase === 'fim') return;
  const alvo = fase === 'coleta' ? ptColeta(ativa) : ptEntrega(ativa);
  const m = metros(eu, alvo), dentro = perto(m);
  $('alvo-txt').textContent = m < 1000 ? Math.round(m) + ' m' : (m / 1000).toFixed(1) + ' km';
  $('btn-coleta').disabled = !dentro;
  $('btn-entregar').disabled = !dentro;
  $('aviso-local').textContent = dentro
    ? (OS_GEO.validar ? 'Você está no local.' : 'Checagem de proximidade desligada nesta instalação.')
    : 'Aproxime-se a menos de ' + RAIO_VALIDACAO_M + ' m para confirmar.';
}

$('btn-coleta').onclick = async () => {
  const codigo = $('cod-retirada').value.trim().toUpperCase();
  if (codigo.length < 6) return aviso('Peça o código de retirada à empresa e digite aqui.', 'erro');
  $('btn-coleta').disabled = true;
  const r = await api('coleta', { lat: eu.lat, lon: eu.lon, codigo });
  if (!r.ok) { aviso(r.dados.erro || 'Não foi possível confirmar a coleta.', 'erro'); return atualizarAlvo(); }
  fase = 'transito';
  simAndando = false;
  if (OS_SIM.ativo) pintarSim(false);
  aviso('Coleta confirmada. Siga para a ' + ativa.ong + (OS_SIM.ativo ? ' (clique em "Simular trajeto").' : '.'));
  mostrarFase();
};

$('foto').onchange = () => {
  const f = $('foto').files[0];
  fotoDemo = null;
  $('preview').classList.toggle('hidden', !f);
  if (f) $('preview').src = URL.createObjectURL(f);
};

$('btn-entregar').onclick = async () => {
  const token = $('token').value.trim().toUpperCase(), foto = $('foto').files[0] || fotoDemo;
  if (token.length < 6) return aviso('Digite o token informado pela ONG.', 'erro');
  if (!foto) return aviso('Tire a foto da descarga antes de validar.', 'erro');
  if (foto.size > 8 * 1024 * 1024) return aviso('A foto passa de 8 MB. Tire outra.', 'erro');

  const f = new FormData();
  f.append('token', token);
  f.append('foto', foto);
  f.append('lat', eu.lat);
  f.append('lon', eu.lon);
  f.append('capturada_em', new Date().toISOString());

  $('btn-entregar').disabled = true;
  const r = await api('finalizar', null, f);
  if (!r.ok) { aviso(r.dados.erro || 'Não foi possível validar a entrega.', 'erro'); return atualizarAlvo(); }

  fase = 'fim';
  $('fim-frete').textContent = brl(ativa.frete);
  if (wake) { wake.release(); wake = null; }
  mostrarFase();
};

$('btn-novo').onclick = () => { location.href = 'entregas'; };

function aoMover(p) {
  meu.setLatLng([eu.lat, eu.lon]);
  if (primeira) { primeira = false; if (ativa) mostrarFase(); } // redesenha a rota a partir da posição real
  if (!ativa) return;
  trilha.addLatLng([eu.lat, eu.lon]);
  atualizarAlvo();
  if (Date.now() - ultimoEnvio > 5000) {
    ultimoEnvio = Date.now();
    api('posicao', { lat: eu.lat, lon: eu.lon, accuracy: p.coords.accuracy });
  }
}

/* ---------- Modo demonstração (app.simulacao=true) ---------- */
let fotoDemo = null, simAndando = false;

/** Lê do servidor a posição gerada pelo simulador e move o veículo no mapa. */
async function seguirSimulacao() {
  const a = await obterAtiva();
  if (!a || !ativa || a.id !== ativa.id) return;
  if (a.fase !== fase && fase !== 'fim') { fase = a.fase; mostrarFase(); }
  if (a.posLat == null) return;
  const novo = { lat: a.posLat, lon: a.posLon }, salto = metros(eu, novo);
  eu = novo;
  if (salto > 500 || primeira) { primeira = false; meu.setLatLng([eu.lat, eu.lon]); mostrarFase(); }
  else osMover(meu, [eu.lat, eu.lon]);
  if (salto > 1) trilha.addLatLng([eu.lat, eu.lon]);
  atualizarAlvo();
  const alvo = fase === 'coleta' ? ptColeta(ativa) : ptEntrega(ativa), chegou = metros(eu, alvo) < 40;
  if (simAndando && chegou) {
    simAndando = false;
    aviso(fase === 'coleta' ? 'Chegou à empresa. Peça o código de retirada.' : 'Chegou à ONG. Peça o token e registre a foto.');
  }
  pintarSim(chegou);
}

function pintarSim(chegou) {
  $('btn-simular').disabled = simAndando || chegou;
  $('sim-txt').textContent = simAndando ? 'A caminho…' : chegou ? 'No destino' : 'Simular trajeto até o destino';
}

$('btn-simular').onclick = async () => {
  $('btn-simular').disabled = true;
  const r = await api('simular', {});
  if (!r.ok) { $('btn-simular').disabled = false; return aviso(r.dados.erro || 'Não foi possível iniciar a simulação.', 'erro'); }
  simAndando = r.dados.estado !== 'chegou';
  pintarSim(r.dados.estado === 'chegou');
  aviso(r.dados.alvo === 'empresa' ? 'A caminho da empresa…' : 'A caminho da ONG…');
};

/** Foto gerada no navegador, para validar a entrega sem câmera durante a apresentação. */
$('btn-foto-demo').onclick = () => {
  const c = document.createElement('canvas'); c.width = 640; c.height = 420;
  const g = c.getContext('2d'), gr = g.createLinearGradient(0, 0, 640, 420);
  gr.addColorStop(0, '#059669'); gr.addColorStop(1, '#34d399');
  g.fillStyle = gr; g.fillRect(0, 0, 640, 420);
  g.fillStyle = 'rgba(255,255,255,.25)';
  for (let i = 0; i < 6; i++) g.fillRect(60 + i * 90, 210 - (i % 3) * 30, 80, 120 + (i % 3) * 30);
  g.fillStyle = '#fff'; g.font = 'bold 34px sans-serif'; g.fillText('Descarga · Lote #' + (ativa ? ativa.id : ''), 40, 70);
  g.font = '24px sans-serif'; g.fillText(ativa ? ativa.ong : '', 40, 110); g.fillText(new Date().toLocaleString('pt-BR'), 40, 145);
  g.font = 'bold 18px sans-serif'; g.fillText('OngSave · imagem de demonstração', 40, 390);
  c.toBlob(b => {
    fotoDemo = new File([b], 'descarga-demo.png', { type: 'image/png' });
    $('foto').value = '';
    $('preview').src = URL.createObjectURL(b); $('preview').classList.remove('hidden');
    aviso('Foto de demonstração anexada.');
  }, 'image/png');
};

(async function iniciar() {
  if (OS_SIM.ativo) {
    $('sim-bloco').classList.remove('hidden');
    $('btn-foto-demo').classList.remove('hidden');
    setInterval(() => { if (ativa && fase !== 'fim' && !document.hidden) seguirSimulacao(); }, 2000);
  }
  iniciarGps(aoMover);
  const a = await obterAtiva();
  if (!a) { $('sem-rota').classList.remove('hidden'); return; }
  await iniciarRota(a, a.fase);
  if (OS_SIM.ativo) seguirSimulacao();
})();
