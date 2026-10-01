<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Dashboard da Instituição - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <link rel="stylesheet" href="${ctx}/assets/css/ong/ong.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="dashboard" data-titulo="Dashboard da Instituição" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">

  <!-- BANNER -->
  <div class="bg-gradient-to-r from-blue-700 to-blue-500 rounded-3xl p-6 md:p-8 text-white shadow-xl shadow-blue-600/20 relative overflow-hidden flex flex-col md:flex-row justify-between items-center gap-6">
    <div class="absolute -right-10 -bottom-10 opacity-10 pointer-events-none"><i class="fa-solid fa-hands-holding-child text-9xl"></i></div>
    <div class="relative z-10 space-y-2">
      <span class="bg-white/20 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider backdrop-blur-md">Recebimento em tempo real</span>
      <h2 id="banner-tit" class="text-2xl lg:text-3xl font-extrabold tracking-tight">Bem-vinda de volta!</h2>
      <p id="banner-resumo" class="text-blue-100 text-sm max-w-xl font-light">Carregando as doações do dia...</p>
    </div>
    <div class="relative z-10 flex flex-wrap gap-3 flex-shrink-0">
      <button id="btn-relatorio" class="no-print py-3.5 px-5 border border-white/60 hover:bg-white/10 font-bold rounded-2xl transition-all text-sm flex items-center gap-2"><i class="fa-solid fa-file-pdf"></i> Relatório de impacto</button>
      <a href="#token" class="py-3.5 px-6 bg-white text-blue-600 hover:bg-blue-50 font-bold rounded-2xl shadow-lg transition-all flex items-center gap-2"><i class="fa-solid fa-key"></i> Validar entrega</a>
    </div>
  </div>

  <div id="alertas" class="space-y-2" aria-live="polite"></div>

  <!-- KPIs -->
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
    <div class="card p-6 hover:shadow-md transition-shadow">
      <div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 grid place-items-center text-xl"><i class="fa-solid fa-weight-scale"></i></div><span id="k-delta" class="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full">--</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Alimentos recebidos</p><h3 id="k-kg" class="skel text-2xl font-extrabold mt-1">0 kg</h3><p id="k-per" class="text-xs text-slate-500 mt-1">--</p>
    </div>
    <div class="card p-6 hover:shadow-md transition-shadow">
      <div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-orange-50 text-orange-600 grid place-items-center text-xl"><i class="fa-solid fa-utensils"></i></div><span class="text-xs font-bold text-orange-600 bg-orange-50 px-2.5 py-1 rounded-full">Fórmula fixa</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Refeições equivalentes</p><h3 id="k-ref" class="skel text-2xl font-extrabold mt-1">0</h3><p id="k-fam" class="text-xs text-slate-500 mt-1">--</p>
    </div>
    <div class="card p-6 hover:shadow-md transition-shadow">
      <div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 grid place-items-center text-xl"><i class="fa-solid fa-box-open"></i></div><span id="k-ativos" class="text-xs font-bold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">--</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Doações recebidas</p><h3 id="k-lotes" class="skel text-2xl font-extrabold mt-1">0</h3><p class="text-xs text-slate-500 mt-1">Lotes validados com código</p>
    </div>
    <div class="card p-6 hover:shadow-md transition-shadow">
      <div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-purple-50 text-purple-600 grid place-items-center text-xl"><i class="fa-solid fa-handshake"></i></div><span class="text-xs font-bold text-purple-600 bg-purple-50 px-2.5 py-1 rounded-full">Rede de parceiros</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Empresas doadoras</p><h3 id="k-emp" class="skel text-2xl font-extrabold mt-1">0</h3><p class="text-xs text-slate-500 mt-1">Que já doaram para a instituição</p>
    </div>
  </div>

  <!-- CAPACIDADE DO DIA -->
  <div class="card p-6">
    <div class="flex flex-wrap items-center justify-between gap-2 mb-3">
      <h3 class="font-bold text-slate-900"><i class="fa-solid fa-warehouse text-blue-600 mr-1"></i> Capacidade de recebimento de hoje</h3>
      <p id="cap-txt" class="text-sm font-semibold text-slate-600"></p>
    </div>
    <div class="h-3 bg-slate-100 rounded-full overflow-hidden"><div id="cap-barra" class="h-full rounded-full bg-blue-500 transition-all duration-700" style="width:0"></div></div>
    <p id="cap-nota" class="text-xs text-slate-500 mt-2"></p>
  </div>

  <!-- MAPA + TOKEN -->
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2">
      <div class="mb-4"><h3 class="text-base font-bold text-slate-900">Doações a caminho</h3><p class="text-xs text-slate-500">Posição enviada pelo app do motorista durante a entrega.</p></div>
      <div class="relative isolate rounded-2xl overflow-hidden border border-slate-200">
        <div id="mapa" class="h-80 w-full"></div>
        <div id="sem-rota" class="hidden absolute inset-0 bg-white/90 z-[500] grid place-items-center text-center text-sm text-slate-500 p-6">
          <div><i class="fa-solid fa-truck text-3xl text-slate-300"></i><p class="mt-2 font-semibold">Nenhuma doação a caminho agora.</p><p>Quando um motorista sair com um lote para vocês, ele aparece aqui.</p></div>
        </div>
      </div>
    </div>

    <div id="token" class="card p-6 scroll-mt-28">
      <h3 class="text-base font-bold text-slate-900"><i class="fa-solid fa-key text-blue-600 mr-1"></i> Código de validação</h3>
      <p class="text-xs text-slate-500 mb-4">Gere o código quando o motorista chegar e passe a ele. A entrega só é validada com o código e a foto da descarga.</p>
      <select id="sel-token" aria-label="Lote na porta" class="campo !mt-0 mb-4 hidden"></select>
      <div id="t-corpo"></div>
    </div>
  </div>

  <!-- CHEGADAS + DISPONÍVEIS -->
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2">
      <h3 class="text-base font-bold text-slate-900">Chegadas em andamento</h3>
      <p class="text-xs text-slate-500 mb-4">O que está vindo, de quem e como preparar o recebimento</p>
      <div id="chegadas" class="space-y-3"></div>
    </div>
    <div class="card p-6">
      <h3 class="text-base font-bold text-slate-900">Propostas de doação</h3>
      <p class="text-xs text-slate-500 mb-4">Aceite antes de o motorista sair. Só aceite o que a instituição consegue receber.</p>
      <div id="disp" class="space-y-3"></div>
    </div>
  </div>

  <!-- GRÁFICOS -->
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2">
      <div class="flex flex-wrap justify-between items-center gap-3 mb-6">
        <div><h3 class="text-base font-bold text-slate-900">Recebimentos</h3><p id="g-sub" class="text-xs text-slate-500">Quilos recebidos</p></div>
        <select id="periodo" aria-label="Período" class="bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs text-slate-600 font-semibold focus:outline-none focus:ring-2 focus:ring-blue-300">
          <option value="30">Últimos 30 dias</option><option value="90">Últimos 90 dias</option><option value="0">Acumulado do ano</option>
        </select>
      </div>
      <div class="h-72"><canvas id="recChart"></canvas></div>
    </div>
    <div class="card p-6 flex flex-col">
      <h3 class="text-base font-bold text-slate-900">Categorias recebidas</h3><p class="text-xs text-slate-500">Para planejar o cardápio e o estoque</p>
      <div class="h-44 my-auto py-3"><canvas id="catChart"></canvas></div>
      <ul class="space-y-1.5 pt-3 border-t border-slate-100 text-xs">
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-blue-500 mr-2"></i>Hortifrúti</span><b>34%</b></li>
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-orange-500 mr-2"></i>Laticínios e frios</span><b>24%</b></li>
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-emerald-500 mr-2"></i>Mercearia</span><b>22%</b></li>
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-violet-500 mr-2"></i>Padaria</span><b>14%</b></li>
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-slate-400 mr-2"></i>Pratos prontos</span><b>6%</b></li>
      </ul>
    </div>
  </div>

  <!-- PARCEIROS + CADASTRO -->
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2">
      <h3 class="text-base font-bold text-slate-900">Empresas que mais doaram</h3><p class="text-xs text-slate-500 mb-4">Quilos recebidos nos últimos 12 meses</p>
      <div id="parceiros" class="space-y-4"></div>
    </div>
    <div class="card p-6">
      <h3 class="text-base font-bold text-slate-900">Cadastro e documentos</h3>
      <dl id="cadastro" class="mt-4 space-y-3 text-sm"></dl>
      <a href="${ctx}/ong/perfil" class="btn-sec block text-center mt-5">Atualizar cadastro</a>
    </div>
  </div>

  <!-- HISTÓRICO -->
  <div class="card overflow-hidden">
    <div class="p-6 border-b border-slate-200 space-y-4">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div><h3 class="text-base font-bold text-slate-900">Histórico de recebimentos</h3><p class="text-xs text-slate-500">Toque em um lote para conferir, ver o detalhe e baixar o recibo</p></div>
        <div class="flex flex-wrap gap-2 no-print">
          <div class="relative"><i class="fa-solid fa-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs"></i>
            <input id="busca" type="search" placeholder="Buscar lote, categoria ou empresa" aria-label="Buscar recebimentos" class="pl-8 pr-3 py-2 w-64 max-w-full bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"></div>
          <button id="btn-csv" class="btn-sec !py-2"><i class="fa-solid fa-file-csv mr-1"></i> CSV</button>
        </div>
      </div>
      <div id="chips" class="flex flex-wrap gap-2 no-print" role="group" aria-label="Filtrar recebimentos"></div>
    </div>
    <div class="overflow-x-auto">
      <table class="w-full text-left border-collapse">
        <thead><tr class="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-200">
          <th class="py-4 px-6">Lote</th><th class="py-4 px-6">Categoria</th><th class="py-4 px-6">Empresa</th><th class="py-4 px-6">Peso recebido</th><th class="py-4 px-6">Situação</th><th class="py-4 px-6 text-right no-print">Ações</th></tr></thead>
        <tbody id="linhas" class="divide-y divide-slate-100 text-sm"></tbody>
      </table>
      <p id="vazio" class="hidden text-center text-sm text-slate-500 py-10">Nenhum recebimento encontrado com esses filtros.</p>
    </div>
  </div>
</section>

<!-- DETALHE -->
<div id="veu-d" class="hidden fixed inset-0 bg-slate-900/40 z-40"></div>
<aside id="drawer" role="dialog" aria-modal="true" aria-labelledby="d-titulo" aria-hidden="true" class="invisible translate-x-full transition-transform fixed top-0 right-0 h-full w-full max-w-md bg-white z-50 shadow-2xl overflow-y-auto">
  <div class="p-6 border-b border-slate-200 flex items-start justify-between sticky top-0 bg-white">
    <div><p class="text-xs font-bold uppercase tracking-wider text-slate-400">Detalhe da doação</p><h2 id="d-titulo" class="text-xl font-extrabold text-slate-900"></h2></div>
    <button id="d-fechar" class="w-10 h-10 rounded-xl bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-blue-600" aria-label="Fechar"><i class="fa-solid fa-xmark"></i></button>
  </div>
  <div id="d-conteudo" class="p-6 space-y-6"></div>
</aside>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/ong/ong.js"></script>
<script src="${ctx}/assets/js/ong/dashboard.js"></script>
</body>

</html>
