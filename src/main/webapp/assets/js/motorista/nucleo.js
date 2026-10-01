/* OngSave · núcleo das páginas do motorista (ofertas, entrega ativa, GPS). API Java:
     GET  /api/motorista/ofertas?lat&lon  -> [{id, empresa, ong, pickupLat, pickupLon, destLat, destLon,
                                               pesoKg, produto, veiculo, validade (ISO), frete}]
     GET  /api/motorista/entrega          -> {entrega: {...mesmos campos, fase: 'coleta'|'transito'}}
     POST /api/motorista/aceitar          id  (409 se outro motorista aceitou primeiro)
     POST /api/motorista/coleta           lat, lon
     POST /api/motorista/posicao          lat, lon, accuracy
     POST /api/motorista/finalizar        multipart: token, foto, lat, lon, capturada_em
   O servidor revalida tudo (raio, veículo, distância, token). O front só filtra por conveniência. */
const RAIO_MAX_KM = PLAT.raioKm; // definido pelo administrador
const FRETE_KEY = { 'Carro Económico': 'carro', 'Camionete': 'camionete', 'Van': 'van', 'Caminhão': 'caminhao' };
const RAIO_VALIDACAO_M = OS_GEO.raioM;
const RANK = { 'Carro Económico': 1, 'Camionete': 2, 'Van': 3, 'Caminhão': 4 };
const CUSTO_KM = { 'Carro Económico': 0.55, 'Camionete': 0.85, 'Van': 1.10, 'Caminhão': 1.80 }; // estimativa de combustível por km
const liquido = o => o.frete - o.dist * (CUSTO_KM[VEICULO] || 1);

const base = OS_BASE;
const csrf = OS_CSRF;
const ptColeta = o => ({ lat: o.pickupLat, lon: o.pickupLon });
const ptEntrega = o => ({ lat: o.destLat, lon: o.destLon });
/* Com geo.validarProximidade=false (desenvolvimento fora de São Paulo) o servidor não exige proximidade. */
const perto = m => !OS_GEO.validar || m <= RAIO_VALIDACAO_M;

function metros(a, b) {
  const r = Math.PI / 180, dLat = (b.lat - a.lat) * r, dLon = (b.lon - a.lon) * r;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(a.lat * r) * Math.cos(b.lat * r) * Math.sin(dLon / 2) ** 2;
  return 12742000 * Math.asin(Math.sqrt(h));
}

const urg = h => h < 24 ? { rot: 'Urgente', badge: 'bg-red-100 text-red-700', borda: 'border-l-red-500', cor: '#ef4444' }
  : h < 48 ? { rot: 'Atenção', badge: 'bg-amber-100 text-amber-700', borda: 'border-l-amber-500', cor: '#f59e0b' }
  : { rot: 'Normal', badge: 'bg-emerald-100 text-emerald-700', borda: 'border-l-emerald-500', cor: '#10b981' };
const fmtHoras = h => h < 1 ? Math.round(h * 60) + ' min' : h < 48 ? Math.floor(h) + ' h' : Math.floor(h / 24) + ' dias';


/* ---------- Comunicação com a API ---------- */
async function api(cam, dados, form) {
  const op = { credentials: 'same-origin', headers: { 'X-CSRF-Token': csrf, 'Accept': 'application/json' } };
  if (dados || form) {
    op.method = 'POST';
    op.body = form || new URLSearchParams(dados);
  }
  try {
    const r = await fetch(base + '/api/motorista/' + cam, op);
    if (r.status === 401) { location.href = base + '/login'; return { ok: false, status: 401, dados: {} }; }
    let d = {}; try { d = await r.json(); } catch (e) { /* sem corpo */ }
    return { ok: r.ok, status: r.status, dados: d };
  } catch (e) {
    return { ok: false, status: 0, dados: { erro: 'Sem conexão. Tente novamente.' } };
  }
}

/* ---------- Estado compartilhado entre as páginas ---------- */
let eu = { lat: -23.5505, lon: -46.6333 };
let gpsOk = false, liberarGps;
const gpsPronto = new Promise(r => { liberarGps = r; setTimeout(r, 4000); }); // sem GPS em 4 s, segue com a região padrão

function iniciarGps(cb) {
  if (OS_SIM.ativo) {                       // modo demonstração: a posição vem do simulador do servidor
    if ($('gps-txt')) $('gps-txt').textContent = 'GPS simulado';
    gpsOk = true; liberarGps();
    return;
  }
  if (!navigator.geolocation) return;
  navigator.geolocation.watchPosition(p => {
    eu = { lat: p.coords.latitude, lon: p.coords.longitude };
    if ($('gps-txt')) $('gps-txt').textContent = 'GPS ativo (±' + Math.round(p.coords.accuracy) + ' m)';
    if (!gpsOk) { gpsOk = true; liberarGps(); }
    if (cb) cb(p);
  }, () => { if ($('gps-txt')) $('gps-txt').textContent = 'Sem GPS: permita a localização'; },
  { enableHighAccuracy: true, maximumAge: 5000 });
}

// Entrega em andamento (GET /api/motorista/entrega).
async function obterAtiva() {
  const r = await api('entrega');
  return r.dados.entrega || null;
}

// Ofertas já filtradas: raio da plataforma (trajeto total), veículo compatível, ainda válidas, mais urgentes primeiro.
async function buscarOfertas() {
  await gpsPronto;
  if (!online()) return [];
  const r = await api('ofertas?lat=' + eu.lat + '&lon=' + eu.lon);
  return (Array.isArray(r.dados) ? r.dados : []).map(o => ({
    ...o,
    dist: (metros(eu, ptColeta(o)) + metros(ptColeta(o), ptEntrega(o))) / 1000,
    horas: (new Date(o.validade) - Date.now()) / 36e5
  })).filter(o => (!OS_GEO.validar || o.dist <= RAIO_MAX_KM) && (RANK[VEICULO] || 3) >= (RANK[o.veiculo] || 1) && o.horas > 0)
    .sort((a, b) => a.horas - b.horas);
}
