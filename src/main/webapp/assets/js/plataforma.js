/* OngSave · base comum a todos os painéis.
   - OS / OS_ESTADO: estado inicial vindo do banco, injetado pela JSP (WEB-INF/jspf/dados.jspf).
   - PLAT: parâmetros da plataforma definidos pelo administrador (tabela plataforma_config).
   - osApi(): chamadas à API JSON do servidor (/api/...), com CSRF e tratamento de sessão expirada.
   - osAcompanhar(): atualização periódica do estado (substitui as antigas simulações).
   Nomes com prefixo OS/os para não colidir com os scripts de cada perfil. */
const OS = (() => { try { return JSON.parse(document.getElementById('os-dados').textContent) || {}; } catch (e) { return {}; } })();
const OS_ESTADO = OS.estado || {};
const OS_BASE = document.body.dataset.base || '';
const OS_CSRF = document.querySelector('meta[name="csrf"]')?.content || '';
const OS_GEO = OS.geo || { validar: false, raioM: 150 };
/* Modo simulação (app.simulacao=true): GPS e frota simulados no servidor e painéis com atualização mais rápida. */
const OS_SIM = OS.sim || { ativo: false, intervaloMs: 3000 };

const PLAT_PADRAO = {
  raioKm: 15,
  fretes: { carro: 25, camionete: 45, van: 70, caminhao: 110 },
  reservaPct: 25,
  planos: {
    pequena: { nome: 'Pequena Empresa', ex: '', preco: 500, franquia: 6, taxaExtra: 55, ret: '' },
    media: { nome: 'Média Empresa', ex: '', preco: 2000, franquia: 20, taxaExtra: 70, ret: '' },
    grande: { nome: 'Grande Empresa', ex: '', preco: 8000, franquia: 60, taxaExtra: 90, ret: '' }
  },
  metodologia: { co2PorKg: 1.69, refeicoesPorKg: 2, fonte: '' },
  regras: { rejeicaoPct: 15, minEntregas: 10, tolPesoPct: 5, concentracaoPct: 60 }
};
const mesclar = (a, b) => {
  const o = { ...a };
  for (const k in (b || {})) o[k] = (b[k] && typeof b[k] === 'object' && !Array.isArray(b[k]) && a[k] && typeof a[k] === 'object') ? mesclar(a[k], b[k]) : b[k];
  return o;
};
const PLAT = mesclar(PLAT_PADRAO, OS.plat || {});

/** Erro devolvido pela API (mensagem já pronta para o utilizador). */
class OsErro extends Error { constructor(msg, status) { super(msg); this.status = status; } }

/**
 * Chama a API. Sem `dados` faz GET; com objeto envia JSON; com FormData envia multipart.
 * Resolve com o JSON da resposta ou lança OsErro com a mensagem do servidor.
 */
async function osApi(caminho, dados) {
  const op = { credentials: 'same-origin', headers: { 'X-CSRF-Token': OS_CSRF, 'Accept': 'application/json' } };
  if (dados !== undefined) {
    op.method = 'POST';
    if (dados instanceof FormData) op.body = dados;
    else { op.headers['Content-Type'] = 'application/json'; op.body = JSON.stringify(dados || {}); }
  }
  let r;
  try { r = await fetch(OS_BASE + '/api/' + caminho, op); }
  catch (e) { throw new OsErro('Sem conexão com o servidor. Tente novamente.', 0); }
  if (r.status === 401) { location.href = OS_BASE + '/login?next=' + encodeURIComponent(location.pathname + location.search); throw new OsErro('Sessão expirada.', 401); }
  let corpo = {};
  try { corpo = await r.json(); } catch (e) { /* sem corpo */ }
  if (!r.ok) throw new OsErro(corpo.erro || 'Não foi possível concluir a operação (erro ' + r.status + ').', r.status);
  return corpo;
}

/** Substitui o conteúdo de um array mantendo a mesma referência (as páginas guardam `const LISTA = ...`). */
function osSubstituir(alvo, novos) { alvo.splice(0, alvo.length, ...(novos || [])); return alvo; }

/** Substitui o conteúdo de um objeto mantendo a referência. */
function osAtribuir(alvo, novo) { Object.keys(alvo).forEach(k => delete alvo[k]); Object.assign(alvo, novo || {}); return alvo; }

/**
 * Atualiza o estado do perfil periodicamente (só com a aba visível) e quando a aba volta a ficar visível.
 * cb(estadoNovo) é chamado a cada leitura bem-sucedida.
 */
function osAcompanhar(perfil, cb, ms) {
  let ocupado = false;
  const ler = async () => {
    if (ocupado || document.hidden) return;
    ocupado = true;
    try { cb(await osApi(perfil + '/estado')); } catch (e) { /* tenta de novo no próximo ciclo */ } finally { ocupado = false; }
  };
  setInterval(ler, OS_SIM.ativo ? Math.min(ms || 10000, OS_SIM.intervaloMs) : (ms || 10000));
  document.addEventListener('visibilitychange', () => { if (!document.hidden) ler(); });
  return ler;
}

/** Marca as notificações do utilizador como lidas no servidor. */
function osMarcarLidas() { osApi('conta/notificacoes/lidas', {}).catch(() => {}); }

/** Grava os parâmetros da plataforma (só administradores). Devolve a configuração validada pelo servidor. */
async function salvarPlat(p) {
  const novo = await osApi('admin/plataforma', p);
  osAtribuir(PLAT, mesclar(PLAT_PADRAO, novo));
  return PLAT;
}

/* ---------- Mapas: movimento suave e rotas pelas ruas ---------- */

/** Desloca um marcador do Leaflet até `destino` ([lat, lon]) em animação, em vez de saltar. */
function osMover(marcador, destino, ms) {
  if (!marcador || !destino || destino[0] == null) return;
  const de = marcador.getLatLng(), dur = ms || (OS_SIM.ativo ? OS_SIM.intervaloMs * 0.95 : 1200);
  cancelAnimationFrame(marcador._osAnim);
  const dLat = destino[0] - de.lat, dLon = destino[1] - de.lng;
  if (Math.abs(dLat) + Math.abs(dLon) < 1e-7) return;
  if (Math.abs(dLat) + Math.abs(dLon) > 0.05) { marcador.setLatLng(destino); return; } // salto grande: sem animação
  const t0 = performance.now();
  const passo = agora => {
    const f = Math.min(1, (agora - t0) / dur);
    marcador.setLatLng([de.lat + dLat * f, de.lng + dLon * f]);
    if (f < 1) marcador._osAnim = requestAnimationFrame(passo);
  };
  marcador._osAnim = requestAnimationFrame(passo);
}

/** Rota de carro entre dois pontos ([lat, lon]) pelo OSRM/OpenStreetMap; sem serviço, linha reta. Com cache. */
const OS_ROTAS = new Map();
function osRota(de, para) {
  const k = [de[0], de[1], para[0], para[1]].map(v => Number(v).toFixed(4)).join(',');
  if (!OS_ROTAS.has(k)) {
    const url = 'https://router.project-osrm.org/route/v1/driving/' + de[1] + ',' + de[0] + ';' + para[1] + ',' + para[0] + '?overview=full&geometries=geojson';
    OS_ROTAS.set(k, fetch(url).then(r => r.json())
      .then(j => [de].concat(j.routes[0].geometry.coordinates.map(c => [c[1], c[0]]), [para]))
      .catch(() => { OS_ROTAS.delete(k); return [de, para]; }));
  }
  return OS_ROTAS.get(k);
}

/* Aviso discreto de que os movimentos são simulados (transparência na apresentação). */
if (OS_SIM.ativo && document.body) {
  const b = document.createElement('div');
  b.setAttribute('role', 'status');
  b.style.cssText = 'position:fixed;left:12px;bottom:12px;z-index:2000;background:#111827;color:#fff;font:600 11px/1.2 Inter,system-ui,sans-serif;padding:7px 11px;border-radius:999px;box-shadow:0 4px 14px rgb(0 0 0/.25);display:flex;align-items:center;gap:7px;opacity:.9;pointer-events:none';
  b.innerHTML = '<span style="width:8px;height:8px;border-radius:50%;background:#22c55e;box-shadow:0 0 0 3px rgb(34 197 94/.3)"></span>Modo demonstração · GPS simulado';
  document.body.appendChild(b);
}
