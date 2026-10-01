<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Painel do Motorista · OngSave</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/motorista/dashboard.css">
</head>
<body class="text-slate-800" data-pagina="geral" data-titulo="Visão Geral" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">

        <div class="rounded-2xl p-6 text-white bg-gradient-to-r from-emerald-600 to-teal-700 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 class="text-2xl font-extrabold">Pronto para rodar hoje?</h2>
            <p class="text-emerald-50 text-sm mt-1"><b class="n-ofertas">0</b> entregas no seu raio de <span class="raio-km">15</span> km, compatíveis com a sua <span class="veic">Van</span>.</p>
          </div>
          <div class="flex flex-wrap gap-3">
            <a id="banner-ativa" href="${ctx}/motorista/rota" class="hidden border border-white/60 text-white font-bold rounded-xl px-5 py-3"><i class="fa-solid fa-route"></i> Continuar rota</a>
            <a href="${ctx}/motorista/entregas" class="bg-white text-emerald-700 font-bold rounded-xl px-5 py-3 shadow"><i class="fa-solid fa-map-location-dot"></i> Ver entregas</a>
          </div>
        </div>

        <div class="grid sm:grid-cols-2 xl:grid-cols-4 gap-4">
          <div class="card stat"><p class="text-xs font-semibold text-slate-400">Ganhos do mês</p><p class="text-2xl font-extrabold mt-1">R$ 1.840,00</p><p class="text-xs text-emerald-600 font-semibold mt-1">+12% · saldo disponível R$ 420,00</p></div>
          <div class="card stat"><p class="text-xs font-semibold text-slate-400">Entregas concluídas</p><p class="text-2xl font-extrabold mt-1">47 entregas</p><p class="text-xs text-slate-500 mt-1">média de R$ 39,15 por frete</p></div>
          <div class="card stat"><p class="text-xs font-semibold text-slate-400">Alimentos resgatados</p><p class="text-2xl font-extrabold mt-1">3.120 kg</p><p class="text-xs text-slate-500 mt-1">equivale a 6.240 refeições</p></div>
          <div class="card stat"><p class="text-xs font-semibold text-slate-400">Confiabilidade</p><p class="text-2xl font-extrabold mt-1">98%</p><p class="text-xs text-slate-500 mt-1">2% de rejeição · conta em dia</p></div>
        </div>

        <div class="grid lg:grid-cols-3 gap-4">
          <div class="card stat lg:col-span-2"><h3 class="font-bold">Ganhos por semana</h3><p class="text-xs text-slate-500 mb-3">Fretes pagos no mês atual</p><div class="relative h-64"><canvas id="c-ganhos"></canvas></div></div>
          <div class="card stat"><h3 class="font-bold">Tipos de carga</h3><p class="text-xs text-slate-500 mb-3">Volume transportado</p><div class="relative h-64"><canvas id="c-cargas"></canvas></div></div>
        </div>

        <div class="grid lg:grid-cols-3 gap-4">
          <div class="card stat lg:col-span-2 overflow-x-auto">
            <h3 class="font-bold">Últimas entregas</h3><p class="text-xs text-slate-500 mb-3">As suas entregas mais recentes. <a href="historico" class="text-emerald-600 font-semibold">Ver histórico</a></p>
            <table class="w-full text-sm text-left">
              <thead class="text-xs text-slate-400"><tr><th class="py-2">Lote</th><th>Rota</th><th>Peso</th><th>Distância</th><th>Frete</th><th>Estado</th></tr></thead>
              <tbody id="ultimas" class="divide-y divide-slate-100"></tbody>
            </table>
          </div>
          <div class="card stat">
            <h3 class="font-bold mb-3">Alertas</h3>
            <ul id="alertas" class="space-y-3 text-sm"></ul>
          </div>
        </div>
      
</section>

<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"></script>
<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/motorista/shell.js"></script>
<script src="${ctx}/assets/js/motorista/nucleo.js"></script>
<script src="${ctx}/assets/js/motorista/dashboard.js"></script>
</body>
</html>
