<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Histórico · OngSave</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/motorista/dashboard.css">
</head>
<body class="text-slate-800" data-pagina="historico" data-titulo="Histórico de Entregas" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">
  <div class="grid sm:grid-cols-3 gap-4">
    <div class="card stat"><p class="rotulo">Fretes no período</p><p id="t-frete" class="text-2xl font-extrabold mt-1"></p></div>
    <div class="card stat"><p class="rotulo">Alimentos entregues</p><p id="t-kg" class="text-2xl font-extrabold mt-1"></p></div>
    <div class="card stat"><p class="rotulo">Distância percorrida</p><p id="t-km" class="text-2xl font-extrabold mt-1"></p></div>
  </div>

  <div class="card stat">
    <div class="flex flex-wrap gap-3 items-end mb-4">
      <label class="rotulo">Estado
        <select id="f-estado" class="campo mt-1">
          <option value="">Todos</option><option>Paga</option><option>Em análise</option><option>Rejeitada</option>
        </select>
      </label>
      <label class="rotulo">Período
        <select id="f-dias" class="campo mt-1">
          <option value="30">Últimos 30 dias</option><option value="90">Últimos 90 dias</option><option value="0">Tudo</option>
        </select>
      </label>
      <label class="rotulo flex-1 min-w-[160px]">Buscar
        <input id="f-busca" class="campo mt-1" placeholder="Lote, empresa ou ONG">
      </label>
      <button id="btn-csv" class="btn-verde"><i class="fa-solid fa-file-arrow-down"></i> Exportar CSV</button>
    </div>
    <p class="text-xs text-slate-500 mb-2">Entregas concluídas e validadas pela ONG.</p>
    <div class="overflow-x-auto">
      <table class="w-full text-sm text-left">
        <thead class="text-xs text-slate-400">
          <tr><th class="py-2">Lote</th><th>Data</th><th>Rota</th><th>Peso</th><th>Distância</th><th>Frete</th><th>Estado</th></tr>
        </thead>
        <tbody id="linhas" class="divide-y divide-slate-100"></tbody>
      </table>
    </div>
    <p id="vazio" class="hidden text-center text-sm text-slate-500 py-8">Nenhuma entrega encontrada com esses filtros.</p>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/motorista/shell.js"></script>
<script src="${ctx}/assets/js/motorista/historico.js"></script>
</body>
</html>
