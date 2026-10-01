<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Entregas Disponíveis · OngSave</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
  <link rel="stylesheet" href="${ctx}/assets/css/motorista/dashboard.css">
</head>
<body class="text-slate-800" data-pagina="entregas" data-titulo="Entregas Disponíveis" data-largo="1" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina">
  <div class="layout grid lg:grid-cols-[400px_1fr]">
    <aside class="p-4 overflow-y-auto min-h-0 bg-slate-50 border-r border-slate-200 order-2 lg:order-1">
      <a id="aviso-ativa" href="${ctx}/motorista/rota" class="hidden mb-3 block rounded-xl bg-emerald-100 text-emerald-800 text-sm font-semibold p-3"><i class="fa-solid fa-route"></i> Você tem uma entrega em andamento. Continuar rota →</a>
<section id="view-ofertas">
        <h1 class="text-lg font-extrabold">
          Entregas disponíveis
          <span id="contagem" class="ml-1 text-xs bg-emerald-600 text-white rounded-full px-2 py-0.5">0</span>
        </h1>
        <p class="text-xs text-slate-500 mb-2">Trajeto total de até <b id="raio-km">15</b> km, compatível com o seu veículo. Você pode recusar qualquer entrega sem penalidade.</p>
        <div class="flex gap-3 text-[11px] text-slate-600 mb-3">
          <span><i class="fa-solid fa-circle text-red-500"></i> vence em menos de 24 h</span>
          <span><i class="fa-solid fa-circle text-amber-500"></i> 24 a 48 h</span>
          <span><i class="fa-solid fa-circle text-emerald-500"></i> mais de 48 h</span>
        </div>
        <div id="lista" class="space-y-3"></div>
        <p id="vazio" class="hidden text-center text-sm text-slate-500 py-10">
          <i class="fa-solid fa-hourglass-half text-2xl text-slate-300"></i><br>
          <span id="vazio-txt">Nenhuma entrega no seu raio agora. Avisamos assim que surgir uma.</span>
        </p>
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
<script src="${ctx}/assets/js/motorista/entregas.js"></script>
</body>
</html>
