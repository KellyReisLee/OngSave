<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Painel do Administrador - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <link rel="stylesheet" href="${ctx}/assets/css/admin/admin.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="dashboard" data-titulo="Controlo Global do Sistema" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">

  <div class="bg-gradient-to-r from-violet-700 to-violet-500 rounded-3xl p-6 md:p-8 text-white shadow-xl shadow-violet-600/20 relative overflow-hidden flex flex-col md:flex-row justify-between items-center gap-6">
    <div class="absolute -right-10 -bottom-10 opacity-10 pointer-events-none"><i class="fa-solid fa-user-shield text-9xl"></i></div>
    <div class="relative z-10 space-y-2">
      <span class="bg-white/20 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider backdrop-blur-md">Central de operação</span>
      <h2 class="text-2xl lg:text-3xl font-extrabold tracking-tight">O que precisa da sua atenção hoje</h2>
      <p id="banner-resumo" class="text-violet-100 text-sm max-w-xl font-light">Carregando o estado da plataforma...</p>
    </div>
    <div class="relative z-10 flex flex-wrap gap-3 flex-shrink-0">
      <a href="${ctx}/admin/financeiro" class="py-3.5 px-5 border border-white/60 hover:bg-white/10 font-bold rounded-2xl transition-all text-sm flex items-center gap-2"><i class="fa-solid fa-scale-balanced"></i> Financeiro</a>
      <a href="${ctx}/admin/gerir-utilizadores?status=pendente" class="py-3.5 px-6 bg-white text-violet-700 hover:bg-violet-50 font-bold rounded-2xl shadow-lg transition-all flex items-center gap-2"><i class="fa-solid fa-user-check"></i> Fila de aprovação</a>
    </div>
  </div>

  <div id="alertas" class="space-y-2" aria-live="polite"></div>

  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-violet-50 text-violet-600 grid place-items-center text-xl"><i class="fa-solid fa-users"></i></div><span id="k-tipos" class="text-xs font-bold text-slate-500 bg-slate-100 px-2.5 py-1 rounded-full">--</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Usuários ativos</p><h3 id="k-ativos" class="skel text-2xl font-extrabold mt-1">0</h3><p class="text-xs text-slate-500 mt-1">Empresas, motoristas e ONGs</p></div>
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 grid place-items-center text-xl"><i class="fa-solid fa-user-clock"></i></div><span id="k-pend-chip" class="text-xs font-bold text-amber-700 bg-amber-50 px-2.5 py-1 rounded-full">Fila</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Aprovações pendentes</p><h3 id="k-pend" class="skel text-2xl font-extrabold mt-1">0</h3><p class="text-xs text-slate-500 mt-1">Cadastros aguardando análise</p></div>
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 grid place-items-center text-xl"><i class="fa-solid fa-truck-fast"></i></div><span class="text-xs font-bold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">Ao vivo</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Lotes em andamento</p><h3 id="k-lotes" class="skel text-2xl font-extrabold mt-1">0</h3><p id="k-lotes-sub" class="text-xs text-slate-500 mt-1">--</p></div>
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 grid place-items-center text-xl"><i class="fa-solid fa-shield-halved"></i></div><span id="k-res-st" class="text-xs font-bold px-2.5 py-1 rounded-full">--</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Reserva de liquidez</p><h3 id="k-res" class="skel text-2xl font-extrabold mt-1">0 meses</h3><p class="text-xs text-slate-500 mt-1">Cobertura de fretes (meta: 3 meses)</p></div>
  </div>

  <div class="card overflow-hidden">
    <div class="p-6 pb-4 flex flex-wrap justify-between items-center gap-3">
      <div><h3 class="text-base font-bold text-slate-900">Entregas em tempo real</h3><p class="text-xs text-slate-500">Posição de cada motorista em coleta ou em trânsito, atualizada automaticamente</p></div>
      <div class="flex flex-wrap gap-3 text-[11px] font-semibold text-slate-500">
        <span class="flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full bg-slate-900"></span>Empresa</span>
        <span class="flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>ONG</span>
        <span class="flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full bg-violet-500"></span>Motorista</span>
        <span id="mapa-n" class="bg-violet-50 text-violet-700 px-2.5 py-1 rounded-full">0 em movimento</span>
      </div>
    </div>
    <div id="mapa-ops" class="h-80 md:h-96 border-t border-slate-200 z-0"></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2"><div class="flex justify-between items-center mb-4"><div><h3 class="text-base font-bold text-slate-900">Fila de aprovação</h3><p class="text-xs text-slate-500">Cadastros novos que dependem de você</p></div><a href="${ctx}/admin/gerir-utilizadores?status=pendente" class="text-xs font-bold text-violet-600 hover:underline">Ver todos</a></div><div id="fila" class="space-y-3"></div></div>
    <div class="card p-6"><h3 class="text-base font-bold text-slate-900">Regras de moderação</h3><p class="text-xs text-slate-500 mb-4">Limites que marcam contas em risco. Mudar recalcula tudo na hora.</p>
      <form id="form-regras" class="space-y-4">
        <div><label class="rotulo" for="r-rej">Rejeição máxima (%)</label><input id="r-rej" type="number" min="1" max="100" class="campo"></div>
        <div><label class="rotulo" for="r-min">Mínimo de entregas para avaliar</label><input id="r-min" type="number" min="1" class="campo"></div>
        <div><label class="rotulo" for="r-tol">Tolerância de peso na conferência (%)</label><input id="r-tol" type="number" min="0" max="50" class="campo"></div>
        <div><label class="rotulo" for="r-con">Concentração motorista-ONG (%)</label><input id="r-con" type="number" min="10" max="100" class="campo"></div>
        <button class="btn-lar w-full" type="submit">Salvar regras</button>
      </form></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2"><div class="mb-4"><h3 class="text-base font-bold text-slate-900">Contas em risco</h3><p class="text-xs text-slate-500">Pelas regras automáticas, com a ação recomendada</p></div><div id="risco" class="space-y-3"></div></div>
    <div class="card p-6"><h3 class="text-base font-bold text-slate-900">Padrões suspeitos</h3><p class="text-xs text-slate-500 mb-4">Concentração de entregas entre o mesmo motorista e a mesma ONG</p><div id="pares" class="space-y-4"></div></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card overflow-hidden lg:col-span-2">
      <div class="p-6 border-b border-slate-200"><h3 class="text-base font-bold text-slate-900">Operações em andamento</h3><p class="text-xs text-slate-500">Lotes publicados, em coleta e em trânsito</p></div>
      <div class="overflow-x-auto"><table class="w-full text-left border-collapse"><thead><tr class="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-200"><th class="py-4 px-6">Lote</th><th class="py-4 px-6">Empresa → ONG</th><th class="py-4 px-6">Motorista</th><th class="py-4 px-6">Situação</th></tr></thead><tbody id="ops" class="divide-y divide-slate-100 text-sm"></tbody></table></div>
    </div>
    <div class="card p-6"><h3 class="text-base font-bold text-slate-900">Atividade recente</h3><p class="text-xs text-slate-500 mb-4">Eventos da plataforma</p><ul id="feed" class="space-y-3 text-sm"></ul></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2"><div class="flex justify-between items-center mb-4"><div><h3 class="text-base font-bold text-slate-900">Ocorrências</h3><p class="text-xs text-slate-500">Divergências de peso, alimento impróprio e fraudes suspeitas</p></div><span id="oc-n" class="text-xs font-bold text-red-700 bg-red-50 px-2.5 py-1 rounded-full"></span></div><div id="ocorr" class="space-y-3"></div></div>
    <div class="card p-6"><h3 class="text-base font-bold text-slate-900">Auditoria recente</h3><p class="text-xs text-slate-500 mb-4">Ações dos administradores</p><ul id="log" class="space-y-3 text-sm"></ul></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2"><h3 class="text-base font-bold text-slate-900">Entregas por semana</h3><p class="text-xs text-slate-500 mb-4">Lotes entregues e validados</p><div class="h-64"><canvas id="entChart"></canvas></div></div>
    <div class="card p-6 flex flex-col"><h3 class="text-base font-bold text-slate-900">Usuários por tipo</h3><p class="text-xs text-slate-500">Contas ativas</p><div class="h-52 my-auto py-4"><canvas id="tipoChart"></canvas></div></div>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/admin/admin.js"></script>
<script src="${ctx}/assets/js/admin/dashboard.js"></script>
</body>

</html>
