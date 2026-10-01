<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Relatórios ESG - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <link rel="stylesheet" href="${ctx}/assets/css/empresa/empresa.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="relatorio-esg" data-titulo="Relatórios e Conformidade ESG" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">
  <div class="flex flex-wrap items-center justify-between gap-3 no-print">
    <p class="text-sm text-slate-500">Métricas de impacto ambiental e socioeconómico validadas por fórmulas matemáticas fixas.</p>
    <div class="flex flex-wrap items-center gap-2">
      <label class="sr-only" for="per">Período</label>
      <select id="per" class="campo !mt-0 !w-auto">
        <option value="tudo">Acumulado até Q1 2026</option>
        <option value="2025">Ano de 2025</option>
        <option value="q1">Q1 2026</option>
      </select>
      <button id="btn-csv" class="btn-sec"><i class="fa-solid fa-file-csv mr-1"></i> CSV</button>
      <button id="btn-pdf" class="btn-lar"><i class="fa-solid fa-file-pdf mr-1"></i> Exportar PDF</button>
    </div>
  </div>

  <div class="bg-gradient-to-r from-emerald-700 to-teal-700 rounded-3xl p-6 md:p-8 text-white shadow-xl shadow-emerald-700/20 relative overflow-hidden flex flex-col md:flex-row justify-between items-center gap-6">
    <div class="absolute -right-8 -bottom-8 opacity-10 pointer-events-none"><i class="fa-solid fa-seedling text-9xl"></i></div>
    <div class="relative z-10 space-y-2 max-w-2xl">
      <span class="bg-white/20 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider">Selo Verde Ativo</span>
      <h2 class="text-2xl lg:text-3xl font-extrabold tracking-tight">Conformidade com Padrões Globais ESG</h2>
      <p class="text-emerald-50 text-sm font-light">Os dados refletem o desvio oficial de resíduos orgânicos de aterros, traduzidos em estimativas de impacto com metodologia transparente. <span id="h-per" class="font-semibold"></span></p>
    </div>
    <div class="relative z-10 text-center bg-white/10 border border-white/20 rounded-2xl px-8 py-4 backdrop-blur">
      <p class="text-[11px] font-bold uppercase tracking-wider text-emerald-100">Índice de eficiência</p>
      <p class="text-4xl font-extrabold">98,4%</p>
      <p class="text-[11px] text-emerald-100">Zero desperdício logístico</p>
    </div>
  </div>

  <div id="aviso-metodo" class="hidden flex gap-3 items-start rounded-2xl border border-amber-200 bg-amber-50 text-amber-900 px-5 py-4 text-sm"></div>

  <div class="card p-6 flex flex-col md:flex-row md:items-center gap-5">
    <div class="w-16 h-16 rounded-2xl bg-slate-100 grid place-items-center text-3xl shrink-0"><i id="n-ic" class="fa-solid fa-medal"></i></div>
    <div class="flex-1">
      <div class="flex flex-wrap justify-between gap-2">
        <h3 class="font-bold text-slate-900">Nível do selo: <span id="n-nome"></span></h3>
        <p id="n-falta" class="text-xs font-semibold text-slate-500"></p>
      </div>
      <div class="h-2.5 bg-slate-100 rounded-full overflow-hidden mt-3"><div id="n-barra" class="h-full bg-emerald-500 rounded-full transition-all duration-700" style="width:0"></div></div>
      <p class="text-[11px] text-slate-400 mt-2">Níveis pelo total acumulado: Bronze 1 t, Prata 3 t, Ouro 5 t e Diamante 10 t de alimentos salvos.</p>
    </div>
  </div>

  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-orange-50 text-orange-600 grid place-items-center text-xl"><i class="fa-solid fa-weight-scale"></i></div><span class="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full">Total acumulado</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Comida salva</p><h3 id="e-kg" class="text-2xl font-extrabold mt-1">0 kg</h3><p id="e-ref" class="text-xs text-slate-500 mt-1"></p></div>
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 grid place-items-center text-xl"><i class="fa-solid fa-cloud-arrow-down"></i></div><span class="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full">Fórmula fixa</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Emissões de CO₂ evitadas</p><h3 id="e-co2" class="text-2xl font-extrabold mt-1">0 t</h3><p class="text-xs text-slate-500 mt-1">Redução da pegada carbónica</p></div>
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 grid place-items-center text-xl"><i class="fa-solid fa-hands-holding-child"></i></div><span class="text-xs font-bold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">Rede social</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">ONGs beneficiadas</p><h3 id="e-ong" class="text-2xl font-extrabold mt-1">0</h3><p class="text-xs text-slate-500 mt-1">Apoio direto a famílias carentes</p></div>
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-purple-50 text-purple-600 grid place-items-center text-xl"><i class="fa-solid fa-trash-can-arrow-up"></i></div><span class="text-xs font-bold text-purple-600 bg-purple-50 px-2.5 py-1 rounded-full">Logística reversa</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Aterros descongestionados</p><h3 id="e-lotes" class="text-2xl font-extrabold mt-1">0 lotes</h3><p class="text-xs text-slate-500 mt-1">Isenção de taxas de destruição</p></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2"><h3 class="font-bold text-slate-900">Redução de CO₂ (toneladas)</h3><p class="text-xs text-slate-500 mb-4">Evolução trimestral acumulada da pegada evitada</p><div class="h-72"><canvas id="co2Chart"></canvas></div></div>
    <div class="card p-6 flex flex-col"><h3 class="font-bold text-slate-900">Categorias de alimento</h3><p class="text-xs text-slate-500">Volume recolhido por setor</p>
      <div class="h-52 my-auto py-4"><canvas id="catChart"></canvas></div>
      <ul class="space-y-2 pt-4 border-t border-slate-100 text-xs">
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-emerald-500 mr-2"></i>Hortifrúti</span><b>40%</b></li>
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-orange-500 mr-2"></i>Laticínios</span><b>25%</b></li>
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-blue-500 mr-2"></i>Padaria</span><b>20%</b></li>
        <li class="flex justify-between"><span><i class="fa-solid fa-circle text-[8px] text-violet-500 mr-2"></i>Mercearia</span><b>15%</b></li>
      </ul></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <div class="card p-6"><h3 class="font-bold text-slate-900">Alinhamento com os ODS da ONU</h3><p class="text-xs text-slate-500 mb-4">Objetivos de Desenvolvimento Sustentável atendidos pelas suas doações</p>
      <div class="flex flex-wrap gap-2 text-xs font-bold">
        <span class="px-3 py-2 rounded-xl bg-amber-50 text-amber-700 border border-amber-200">ODS 2 · Fome zero</span>
        <span class="px-3 py-2 rounded-xl bg-orange-50 text-orange-700 border border-orange-200">ODS 12 · Consumo e produção responsáveis</span>
        <span class="px-3 py-2 rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-200">ODS 13 · Ação contra a mudança do clima</span>
      </div></div>
    <div class="card p-6"><h3 class="font-bold text-slate-900">Como calculamos</h3><p class="text-xs text-slate-500 mb-4">Fatores fixos, definidos pela plataforma, sem inteligência artificial</p>
      <ul id="metodo" class="space-y-2 text-sm text-slate-600"></ul></div>
  </div>

  <div class="card overflow-hidden">
    <div class="p-6 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3">
      <div><h3 class="font-bold text-slate-900">Histórico de Auditoria ESG</h3><p class="text-xs text-slate-500">Dados imutáveis gerados após validação com Token da ONG</p></div>
      <div class="relative no-print"><i class="fa-solid fa-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs"></i>
        <input id="busca" type="search" placeholder="Buscar lote, categoria ou ONG" aria-label="Buscar na auditoria" class="pl-8 pr-3 py-2 w-64 max-w-full bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-300"></div>
    </div>
    <div class="overflow-x-auto">
      <table class="w-full text-left border-collapse">
        <thead><tr class="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-200">
          <th class="py-4 px-6">Data / ID</th><th class="py-4 px-6">Categoria</th><th class="py-4 px-6">Peso salvo</th><th class="py-4 px-6">CO₂ evitado</th><th class="py-4 px-6">Instituição receptora</th><th class="py-4 px-6">Auditoria</th></tr></thead>
        <tbody id="audit" class="divide-y divide-slate-100 text-sm"></tbody>
      </table>
      <p id="audit-vazio" class="hidden text-center text-sm text-slate-500 py-10"></p>
    </div>
  </div>
</section>


<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/empresa/empresa.js"></script>
<script src="${ctx}/assets/js/empresa/relatorio-esg.js"></script>
</body>

</html>
