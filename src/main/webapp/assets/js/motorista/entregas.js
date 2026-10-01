/* Página Entregas Disponíveis: lista de ofertas e mapa */
let ofertas = [], vistas = null, primeira = true;
const map = L.map('map').setView([eu.lat, eu.lon], 13);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(map);
const meu = L.marker([eu.lat, eu.lon], {
  icon: L.divIcon({ className: '', html: '<div class="eu"></div>', iconSize: [16, 16] }), zIndexOffset: 1000
}).addTo(map);
const pin = cor => L.divIcon({ className: '', html: '<div class="pin" style="background:' + cor + '"></div>', iconSize: [22, 22] });
const camOfertas = L.layerGroup().addTo(map), camPrev = L.layerGroup().addTo(map);

async function carregarOfertas() {
  ofertas = await buscarOfertas();
  if (vistas) ofertas.filter(o => !vistas.has(o.id)).forEach(o => aviso('Nova entrega no seu raio: ' + o.empresa));
  vistas = new Set(ofertas.map(o => o.id));
  $('vazio-txt').textContent = online()
    ? 'Nenhuma entrega no seu raio agora. Avisamos assim que surgir uma.'
    : 'Você está offline. Fique online para ver e receber entregas.';
  renderOfertas();
}

function renderOfertas() {
  camOfertas.clearLayers(); camPrev.clearLayers();
  $('contagem').textContent = ofertas.length;
  document.querySelectorAll('.n-ofertas').forEach(e => { e.textContent = ofertas.length; });
  $('vazio').classList.toggle('hidden', ofertas.length > 0);
  $('lista').innerHTML = '';
  ofertas.forEach(o => {
    const u = urg(o.horas);
    const c = document.createElement('article');
    c.className = 'card border-l-4 ' + u.borda;
    c.id = 'of-' + o.id;
    c.dataset.id = o.id;
    c.innerHTML =
      '<div class="flex items-start justify-between gap-2"><div>' +
        '<h3 class="font-bold leading-tight">' + esc(o.empresa) + '</h3>' +
        '<p class="text-xs text-slate-500"><i class="fa-solid fa-arrow-right"></i> ' + esc(o.ong) + '</p></div>' +
        '<span class="badge ' + u.badge + '">' + u.rot + '</span></div>' +
      '<div class="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-600">' +
        '<span><i class="fa-solid fa-weight-hanging"></i> ' + esc(o.pesoKg) + ' kg · ' + esc(o.produto) + '</span>' +
        '<span><i class="fa-solid fa-truck"></i> ' + esc(o.veiculo) + '</span>' +
        '<span><i class="fa-solid fa-route"></i> ' + o.dist.toFixed(1) + ' km</span>' +
        '<span><i class="fa-regular fa-clock"></i> vence em ' + fmtHoras(o.horas) + '</span></div>' +
      '<div class="mt-3 flex items-center justify-between">' +
        '<div><span class="text-lg font-extrabold text-emerald-600">' + brl(o.frete) + '</span>' +
        '<p class="text-[11px] text-slate-500">líquido ≈ ' + brl(liquido(o)) + ' · ' + brl(o.frete / o.dist) + '/km</p></div>' +
        '<button class="btn-aceitar btn-verde">Aceitar entrega</button></div>';
    $('lista').appendChild(c);
    L.marker([o.pickupLat, o.pickupLon], { icon: pin(u.cor) }).addTo(camOfertas)
      .bindTooltip(o.empresa).on('click', () => destacar(o.id));
  });
}

$('lista').onclick = e => {
  const c = e.target.closest('article');
  if (!c) return;
  if (e.target.closest('.btn-aceitar')) aceitar(c.dataset.id); else destacar(c.dataset.id);
};

function destacar(id) {
  const o = ofertas.find(x => String(x.id) === String(id));
  if (!o) return;
  document.querySelectorAll('#lista article').forEach(a => a.classList.toggle('sel', a.dataset.id === String(id)));
  $('of-' + id)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  camPrev.clearLayers();
  L.polyline([[o.pickupLat, o.pickupLon], [o.destLat, o.destLon]], { color: '#64748b', weight: 3, dashArray: '6 8' }).addTo(camPrev);
  L.marker([o.destLat, o.destLon], { icon: pin('#3b82f6') }).addTo(camPrev).bindTooltip(o.ong, { permanent: true });
  map.fitBounds(L.latLngBounds([[eu.lat, eu.lon], [o.pickupLat, o.pickupLon], [o.destLat, o.destLon]]).pad(0.25));
}

async function aceitar(id) {
  if (await obterAtiva()) return aviso('Você já tem uma entrega em andamento. Conclua-a antes.', 'erro');
  const o = ofertas.find(x => String(x.id) === String(id));
  if (!o || !confirm('Aceitar a coleta em ' + o.empresa + '? Frete: ' + brl(o.frete))) return;
  if (!(await garantirConsentimento())) return aviso('Sem o consentimento de localização não é possível aceitar entregas.', 'erro');
  const r = await api('aceitar', { id: o.id });
  if (!r.ok) {
    aviso(r.status === 409 ? 'Outro motorista aceitou primeiro.' : (r.dados.erro || 'Não foi possível aceitar.'), 'erro');
    return carregarOfertas();
  }
  location.href = 'rota';
}

iniciarGps(() => {
  meu.setLatLng([eu.lat, eu.lon]);
  if (primeira) { primeira = false; map.setView([eu.lat, eu.lon], 13); }
});
carregarOfertas();
setInterval(carregarOfertas, 20000);
window.addEventListener('online-mudou', carregarOfertas);
obterAtiva().then(a => { if (a) $('aviso-ativa').classList.remove('hidden'); });

$('raio-km').textContent = RAIO_MAX_KM;
