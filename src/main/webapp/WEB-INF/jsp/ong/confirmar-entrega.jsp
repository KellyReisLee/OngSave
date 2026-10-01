<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Confirmar Entrega - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/ong/ong.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="confirmar-entrega" data-titulo="Confirmar Entrega" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">

  <div class="card p-6"><ol id="passos" class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4" aria-label="Etapas da confirmação"></ol></div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
    <div class="card p-6">
      <h3 class="text-base font-bold text-slate-900">Motoristas</h3>
      <p class="text-xs text-slate-500">Selecione o lote que vai validar</p>
      <div id="lista" class="space-y-3 mt-4"></div>
    </div>
    <div id="painel" class="card p-6 md:p-8 lg:col-span-2" aria-live="polite"></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card overflow-hidden lg:col-span-2">
      <div class="p-6 border-b border-slate-200 flex flex-wrap justify-between items-center gap-2">
        <div><h3 class="text-base font-bold text-slate-900">Últimas validações</h3><p class="text-xs text-slate-500">Os cinco recebimentos mais recentes</p></div>
        <a href="${ctx}/ong/dashboard" class="text-xs font-bold text-blue-600 hover:underline">Ver histórico completo <i class="fa-solid fa-arrow-right text-[10px]"></i></a>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead><tr class="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-200">
            <th class="py-4 px-6">Lote</th><th class="py-4 px-6">Empresa</th><th class="py-4 px-6">Peso</th><th class="py-4 px-6">Situação</th></tr></thead>
          <tbody id="ultimas" class="divide-y divide-slate-100 text-sm"></tbody>
        </table>
      </div>
    </div>
    <div class="card p-6">
      <h3 class="text-base font-bold text-slate-900"><i class="fa-solid fa-shield-halved text-blue-600 mr-1"></i> Segurança da entrega</h3>
      <ul class="mt-4 space-y-3 text-sm text-slate-600">
        <li class="flex gap-3"><i class="fa-solid fa-user-check text-emerald-600 mt-0.5"></i> Confira o nome do motorista e a placa antes de gerar o código.</li>
        <li class="flex gap-3"><i class="fa-solid fa-hand-holding text-emerald-600 mt-0.5"></i> Passe o código somente ao motorista, pessoalmente.</li>
        <li class="flex gap-3"><i class="fa-solid fa-stopwatch text-emerald-600 mt-0.5"></i> O código vale 10 minutos e só pode ser usado uma vez.</li>
        <li class="flex gap-3"><i class="fa-solid fa-camera text-emerald-600 mt-0.5"></i> Sem a foto da descarga, a entrega não é validada.</li>
        <li class="flex gap-3"><i class="fa-solid fa-triangle-exclamation text-amber-500 mt-0.5"></i> Se algo estiver errado, não gere o código e reporte o problema.</li>
      </ul>
    </div>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/ong/ong.js"></script>
<script src="${ctx}/assets/js/ong/confirmar-entrega.js"></script>
</body>

</html>
