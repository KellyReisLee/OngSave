<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Rota Ativa · OngSave</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
  <link rel="stylesheet" href="${ctx}/assets/css/motorista/dashboard.css">
</head>
<body class="text-slate-800" data-pagina="rota" data-titulo="Rota Ativa" data-largo="1" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina">
  <div class="layout grid lg:grid-cols-[400px_1fr]">
    <aside class="p-4 overflow-y-auto min-h-0 bg-slate-50 border-r border-slate-200 order-2 lg:order-1">
      <div id="sem-rota" class="hidden text-center py-10">
        <i class="fa-solid fa-route text-3xl text-slate-300"></i>
        <p class="mt-3 font-bold">Nenhuma rota ativa</p>
        <p class="text-sm text-slate-500 mb-4">Aceite uma entrega para começar.</p>
        <a href="${ctx}/motorista/entregas" class="btn-verde">Ver entregas disponíveis</a>
      </div>
<section id="view-rota" class="hidden">
        <ol class="flex mb-4">
          <li id="s1" class="passo"><span class="bola"><i class="fa-solid fa-warehouse"></i></span>Coleta</li>
          <li id="s2" class="passo"><span class="bola"><i class="fa-solid fa-truck-fast"></i></span>Em trânsito</li>
          <li id="s3" class="passo"><span class="bola"><i class="fa-solid fa-check"></i></span>Entregue</li>
        </ol>

        <div class="card cursor-default">
          <h2 id="rota-empresa" class="font-extrabold text-lg"></h2>
          <p id="rota-resumo" class="text-xs text-slate-500 mt-1"></p>
        </div>

        <div id="bloco-alvo" class="mt-3 bg-white border border-slate-200 rounded-xl p-3">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-xs font-semibold text-slate-400">Distância até o destino</p>
              <p id="alvo-txt" class="text-2xl font-extrabold text-emerald-600">--</p>
            </div>
            <a id="nav-link" target="_blank" rel="noopener" class="btn-verde">
              <i class="fa-solid fa-diamond-turn-right"></i> Navegar
            </a>
          </div>
          <p id="aviso-local" class="text-xs text-slate-500 mt-2"></p>
          <div id="sim-bloco" class="hidden mt-3 pt-3 border-t border-dashed border-slate-200">
            <button id="btn-simular" type="button" class="w-full py-2.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-white text-sm font-bold flex items-center justify-center gap-2 disabled:opacity-60">
              <i class="fa-solid fa-play"></i> <span id="sim-txt">Simular trajeto até o destino</span>
            </button>
            <p class="text-[11px] text-slate-400 mt-1.5 text-center">Modo demonstração: o veículo percorre a rota pelas ruas e a empresa, a ONG e o admin acompanham ao vivo.</p>
          </div>
        </div>

        <div id="p-coleta" class="mt-3">
          <p class="text-sm text-slate-600 mb-2">Vá até a empresa, confira o peso e peça o <b>código de retirada</b>. Só com o código a coleta é confirmada.</p>
          <label class="block text-xs font-semibold text-slate-500 mb-3">Código de retirada (informado pela empresa)
            <input id="cod-retirada" maxlength="8" autocomplete="off" placeholder="Ex.: K7Q2ZA" class="mt-1 w-full border border-slate-300 rounded-lg px-3 py-2 text-lg tracking-widest uppercase font-mono focus:outline-none focus:border-emerald-500">
          </label>
          <button id="btn-coleta" class="btn-verde w-full py-3" disabled>
            <i class="fa-solid fa-box-open"></i> Confirmar coleta
          </button>
        </div>

        <div id="p-transito" class="mt-3 hidden space-y-3">
          <p class="text-sm text-slate-600">Ao chegar na ONG, peça o token digital e fotografe os mantimentos descarregados.</p>
          <label class="block text-xs font-semibold text-slate-500">Token da ONG
            <input id="token" maxlength="12" autocomplete="off" placeholder="Ex.: A7K29Q"
                   class="mt-1 w-full border border-slate-300 rounded-lg px-3 py-2 text-lg tracking-widest uppercase font-mono focus:outline-none focus:border-emerald-500">
          </label>
          <label class="block cursor-pointer border-2 border-dashed border-slate-300 rounded-xl p-4 text-center text-sm text-slate-500 hover:border-emerald-500">
            <i class="fa-solid fa-camera"></i> Tirar foto da descarga
            <input id="foto" type="file" accept="image/*" capture="environment" class="hidden">
            <img id="preview" alt="Prévia da foto" class="hidden mt-3 rounded-lg mx-auto max-h-40">
          </label>
          <button id="btn-foto-demo" type="button" class="hidden w-full text-xs font-semibold text-slate-500 hover:text-emerald-600 underline">Usar foto de demonstração</button>
          <button id="btn-entregar" class="btn-verde w-full py-3" disabled>
            <i class="fa-solid fa-circle-check"></i> Validar entrega
          </button>
        </div>

        <div id="p-fim" class="mt-3 hidden text-center card cursor-default">
          <i class="fa-solid fa-circle-check text-5xl text-emerald-500"></i>
          <h3 class="font-extrabold text-lg mt-2">Entrega validada</h3>
          <p class="text-sm text-slate-600">Frete de <b id="fim-frete"></b> liberado na sua carteira.</p>
          <button id="btn-novo" class="btn-verde mt-4">Ver novas entregas</button>
        </div>
      </section>
    </aside>
    <div class="order-1 lg:order-2 min-h-0"><div id="map"></div></div>
  </div>
</section>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/motorista/shell.js"></script>
<script src="${ctx}/assets/js/motorista/nucleo.js"></script>
<script src="${ctx}/assets/js/motorista/rota.js"></script>
</body>
</html>
