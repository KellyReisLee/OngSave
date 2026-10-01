<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Dashboard Corporativo - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <link rel="stylesheet" href="${ctx}/assets/css/empresa/empresa.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="dashboard" data-titulo="Dashboard Operacional" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">

      <!-- BANNER -->
      <div class="bg-gradient-to-r from-orange-600 to-amber-600 rounded-3xl p-6 md:p-8 text-white shadow-xl shadow-orange-600/20 relative overflow-hidden flex flex-col md:flex-row justify-between items-center gap-6">
        <div class="absolute -right-10 -bottom-10 opacity-10 pointer-events-none"><i class="fa-solid fa-hand-holding-heart text-9xl"></i></div>
        <div class="relative z-10 space-y-2">
          <span class="bg-white/20 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider backdrop-blur-md">Impacto em Tempo Real</span>
          <h2 class="text-2xl lg:text-3xl font-extrabold tracking-tight">Pronto para destinar excedentes hoje?</h2>
          <p id="banner-resumo" class="text-orange-100 text-sm max-w-xl font-light">Carregando o estado dos seus lotes...</p>
        </div>
        <div class="relative z-10 flex flex-wrap gap-3 flex-shrink-0">
          <a href="${ctx}/empresa/relatorio-esg" class="py-3.5 px-5 border border-white/60 hover:bg-white/10 font-bold rounded-2xl transition-all text-sm flex items-center gap-2"><i class="fa-solid fa-chart-line"></i> Relatório ESG</a>
          <a href="${ctx}/empresa/nova-doacao" class="py-3.5 px-6 bg-white text-orange-600 hover:bg-orange-50 font-bold rounded-2xl shadow-lg transition-all flex items-center gap-2"><i class="fa-solid fa-plus-circle text-lg"></i> Registar Novo Lote</a>
        </div>
      </div>

      <!-- ALERTAS -->
      <div id="alertas" class="space-y-2" aria-live="polite"></div>

      <!-- KPIs -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div class="card p-6 hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-4">
            <div class="w-12 h-12 rounded-2xl bg-orange-50 text-orange-600 flex items-center justify-center text-xl"><i class="fa-solid fa-weight-scale"></i></div>
            <span id="k-delta" class="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full">--</span>
          </div>
          <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Alimentos Salvos</p>
          <h3 id="k-kg" class="skel text-2xl font-extrabold text-slate-900 mt-1">0 kg</h3>
          <p id="k-ref" class="text-xs text-slate-500 mt-1">--</p>
        </div>
        <div class="card p-6 hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-4">
            <div class="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-xl"><i class="fa-solid fa-cloud-arrow-down"></i></div>
            <span class="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full">Meta ESG OK</span>
          </div>
          <p class="text-xs font-bold uppercase tracking-wider text-slate-400">CO₂ Evitado</p>
          <h3 id="k-co2" class="skel text-2xl font-extrabold text-slate-900 mt-1">0 t</h3>
          <p class="text-xs text-slate-500 mt-1">Fórmula fixa por quilo resgatado</p>
        </div>
        <div class="card p-6 hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-4">
            <div class="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center text-xl"><i class="fa-solid fa-truck-fast"></i></div>
            <span id="k-ativos" class="text-xs font-bold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">--</span>
          </div>
          <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Lotes no Período</p>
          <h3 id="k-lotes" class="skel text-2xl font-extrabold text-slate-900 mt-1">0</h3>
          <p class="text-xs text-slate-500 mt-1">Publicados e entregues</p>
        </div>
        <div class="card p-6 hover:shadow-md transition-shadow">
          <div class="flex justify-between items-start mb-4">
            <div class="w-12 h-12 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center text-xl"><i class="fa-solid fa-hand-holding-dollar"></i></div>
            <span id="k-plano" class="text-xs font-bold text-purple-600 bg-purple-50 px-2.5 py-1 rounded-full">Plano</span>
          </div>
          <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Economia Líquida</p>
          <h3 id="k-eco" class="skel text-2xl font-extrabold text-slate-900 mt-1">R$ 0</h3>
          <p id="k-eco-sub" class="text-xs text-slate-500 mt-1">Seu custo de descarte menos o plano</p>
        </div>
      </div>

      <!-- INDICADORES OPERACIONAIS -->
      <div class="card grid grid-cols-3 divide-x divide-slate-100 text-center">
        <div class="p-4"><p class="text-xl font-extrabold">18 min</p><p class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Tempo até o aceite</p></div>
        <div class="p-4"><p class="text-xl font-extrabold">96%</p><p class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Lotes entregues</p></div>
        <div class="p-4"><p class="text-xl font-extrabold">12</p><p class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">ONGs atendidas</p></div>
      </div>

      <!-- FRANQUIA E DOSSIÊ FISCAL -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card p-6">
          <div class="flex flex-wrap items-center justify-between gap-2 mb-3"><h3 class="font-bold text-slate-900"><i class="fa-solid fa-ticket text-orange-600 mr-1"></i> Franquia de lotes do plano</h3><p id="pl-txt" class="text-sm font-semibold text-slate-600"></p></div>
          <div class="h-3 bg-slate-100 rounded-full overflow-hidden"><div id="pl-barra" class="h-full rounded-full bg-orange-500 transition-all duration-700" style="width:0"></div></div>
          <p id="pl-nota" class="text-xs text-slate-500 mt-2"></p>
        </div>
        <div class="card p-6">
          <h3 class="font-bold text-slate-900"><i class="fa-solid fa-file-invoice text-orange-600 mr-1"></i> Dossiê de doações, Selo Doador e dedução</h3>
          <p class="text-xs text-slate-500 mt-1">A Lei 15.224/2025 criou o Selo Doador de Alimentos e elevou para 5% o limite de dedução no IRPJ nas doações de alimentos. Quem usar a dedução terá de informar as doações a um sistema de registro, na forma do regulamento. Confirme as regras com o seu contador.</p>
          <p id="fs-n" class="text-sm font-semibold text-slate-700 mt-3"></p>
          <button id="btn-dossie" class="btn-sec mt-3 !py-2 text-xs"><i class="fa-solid fa-file-csv mr-1"></i> Exportar dossiê (CSV)</button>
        </div>
      </div>

      <!-- RASTREIO AO VIVO -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="card p-6 lg:col-span-2">
          <div class="flex flex-wrap justify-between items-center gap-3 mb-4">
            <div>
              <h3 class="text-base font-bold text-slate-900">Acompanhamento ao Vivo</h3>
              <p class="text-xs text-slate-500">Posição enviada pelo app do motorista durante a entrega.</p>
            </div>
            <select id="sel-lote" aria-label="Lote em trânsito" class="bg-slate-50 border border-slate-200 rounded-xl px-3 py-1.5 text-xs text-slate-600 font-semibold focus:outline-none"></select>
          </div>
          <div class="relative isolate rounded-2xl overflow-hidden border border-slate-200">
            <div id="mapa" class="h-80 w-full"></div>
            <div id="sem-transito" class="hidden absolute inset-0 bg-white/90 z-[500] grid place-items-center text-center text-sm text-slate-500 p-6">
              <div><i class="fa-solid fa-truck text-3xl text-slate-300"></i><p class="mt-2 font-semibold">Nenhum lote em trânsito agora.</p><p>Quando um motorista coletar, o trajeto aparece aqui.</p></div>
            </div>
          </div>
        </div>

        <div class="card p-6">
          <h3 id="t-titulo" class="text-base font-bold text-slate-900">Estado do lote</h3>
          <div id="t-info" class="mt-4 space-y-5 text-sm"></div>
        </div>
      </div>

      <!-- GRÁFICOS -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="card p-6 lg:col-span-2">
          <div class="flex flex-wrap justify-between items-center gap-3 mb-6">
            <div>
              <h3 class="text-base font-bold text-slate-900">Evolução de Resgates</h3>
              <p id="g-sub" class="text-xs text-slate-500">Desvio de resíduos para aterros</p>
            </div>
            <div id="metricas" class="flex gap-2" role="group" aria-label="Métrica do gráfico">
              <button data-m="kg" class="chip px-3 py-1.5 rounded-full text-xs font-bold border border-slate-200 text-slate-600 hover:bg-slate-50" aria-pressed="true">Quilos</button>
              <button data-m="lotes" class="chip px-3 py-1.5 rounded-full text-xs font-bold border border-slate-200 text-slate-600 hover:bg-slate-50" aria-pressed="false">Lotes</button>
              <button data-m="co2" class="chip px-3 py-1.5 rounded-full text-xs font-bold border border-slate-200 text-slate-600 hover:bg-slate-50" aria-pressed="false">CO₂</button>
            </div>
          </div>
          <div class="h-72 w-full"><canvas id="esgChart"></canvas></div>
        </div>

        <div class="card p-6 flex flex-col">
          <h3 class="text-base font-bold text-slate-900">Veículos Usados</h3>
          <p class="text-xs text-slate-500">Demanda logística por categoria</p>
          <div class="h-52 w-full my-auto py-4"><canvas id="vehicleChart"></canvas></div>
          <ul class="space-y-2 pt-4 border-t border-slate-100 text-xs">
            <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-orange-500 mr-2"></i>Carro / Utilitário</span><b>15%</b></li>
            <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-slate-900 mr-2"></i>Van / Furgão</span><b>45%</b></li>
            <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-orange-300 mr-2"></i>Caminhão de carga</span><b>40%</b></li>
          </ul>
        </div>
      </div>

      <!-- LOTES -->
      <div class="card overflow-hidden">
        <div class="p-6 border-b border-slate-200 space-y-4">
          <div class="flex flex-wrap justify-between items-center gap-3">
            <div>
              <h3 class="text-base font-bold text-slate-900">Lotes Registados</h3>
              <p class="text-xs text-slate-500">Toque em um lote para ver o detalhe e o histórico</p>
            </div>
            <div class="relative">
              <i class="fa-solid fa-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs"></i>
              <input id="busca" type="search" placeholder="Buscar lote, tipo ou ONG" aria-label="Buscar lotes"
                class="pl-8 pr-3 py-2 w-64 max-w-full bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-300">
            </div>
          </div>
          <div id="chips" class="flex flex-wrap gap-2" role="group" aria-label="Filtrar por estado"></div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-200">
                <th class="py-4 px-6">ID / Lote</th>
                <th class="py-4 px-6">Tipo de Alimento</th>
                <th class="py-4 px-6">Peso</th>
                <th class="py-4 px-6">Veículo</th>
                <th class="py-4 px-6">ONG Destino</th>
                <th class="py-4 px-6">Estado Atual</th>
                <th class="py-4 px-6 text-right">Ações</th>
              </tr>
            </thead>
            <tbody id="linhas" class="divide-y divide-slate-100 text-sm"></tbody>
          </table>
          <p id="vazio" class="hidden text-center text-sm text-slate-500 py-10">Nenhum lote encontrado com esses filtros.</p>
        </div>
      </div>
    </section>

<!-- DETALHE DO LOTE -->
  <div id="veu-d" class="hidden fixed inset-0 bg-slate-900/40 z-40"></div>
  <aside id="drawer" role="dialog" aria-modal="true" aria-labelledby="d-titulo" aria-hidden="true"
    class="invisible translate-x-full transition-transform fixed top-0 right-0 h-full w-full max-w-md bg-white z-50 shadow-2xl overflow-y-auto">
    <div class="p-6 border-b border-slate-200 flex items-start justify-between sticky top-0 bg-white">
      <div>
        <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Detalhe do lote</p>
        <h2 id="d-titulo" class="text-xl font-extrabold text-slate-900"></h2>
      </div>
      <button id="d-fechar" class="w-10 h-10 rounded-xl bg-slate-100 text-slate-600 hover:bg-orange-50 hover:text-orange-600" aria-label="Fechar"><i class="fa-solid fa-xmark"></i></button>
    </div>
    <div id="d-conteudo" class="p-6 space-y-6"></div>
  </aside>

  
<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/empresa/empresa.js"></script>
<script src="${ctx}/assets/js/empresa/dashboard.js"></script>
</body>

</html>
