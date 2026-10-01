<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Detalhe do Utilizador - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/admin/admin.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="usuarios" data-titulo="Inspeção de Cadastro" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">
  <a href="${ctx}/admin/gerir-utilizadores" class="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-violet-600"><i class="fa-solid fa-arrow-left"></i> Voltar à lista</a>

  <div id="nao-achou" class="hidden card p-10 text-center text-slate-500"><i class="fa-solid fa-user-slash text-4xl text-slate-300"></i><p class="mt-3 font-bold text-slate-800">Utilizador não encontrado</p><p class="text-sm">Volte à lista e abra o cadastro de novo.</p></div>

  <div id="conteudo" class="space-y-6">
    <div id="cab" class="card p-6 md:p-8"></div>
    <div id="reco" class="hidden flex items-start gap-3 px-5 py-4 rounded-2xl border border-red-200 bg-red-50 text-red-800 text-sm"></div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
      <div class="lg:col-span-2 space-y-6">
        <div class="card p-6 md:p-8"><h3 class="font-bold text-slate-900 mb-4"><i class="fa-solid fa-address-card text-violet-600 mr-1"></i> Dados cadastrais</h3><dl id="dados" class="grid sm:grid-cols-2 gap-x-8 gap-y-4 text-sm"></dl></div>
        <div class="card p-6 md:p-8"><div class="flex flex-wrap justify-between items-center gap-3 mb-2"><div><h3 class="font-bold text-slate-900"><i class="fa-solid fa-file-shield text-violet-600 mr-1"></i> Documentos</h3><p class="text-xs text-slate-500">Revise cada documento. O cadastro só pode ser aprovado com todos aprovados.</p></div>
          <button id="btn-pedir" class="btn-sec !py-2 text-xs"><i class="fa-solid fa-paper-plane mr-1"></i> Solicitar documento</button></div><ul id="docs" class="divide-y divide-slate-100"></ul></div>
        <div class="card p-6 md:p-8"><h3 class="font-bold text-slate-900 mb-4"><i class="fa-solid fa-clock-rotate-left text-violet-600 mr-1"></i> Histórico</h3><ol id="hist" class="space-y-4 text-sm"></ol></div>
      </div>

      <div class="space-y-6">
        <div class="card p-6"><h3 class="font-bold text-slate-900 mb-4">Ações</h3><div id="acoes" class="space-y-3"></div></div>
        <div class="card p-6"><h3 class="font-bold text-slate-900 mb-4">Desempenho</h3><dl id="metricas" class="space-y-3 text-sm"></dl></div>
        <div class="card p-6"><h3 class="font-bold text-slate-900 mb-1">Sinais de risco</h3><p class="text-xs text-slate-500 mb-4">Pelas regras automáticas da plataforma</p><ul id="sinais" class="space-y-2 text-sm"></ul></div>
        <div class="card p-6"><h3 class="font-bold text-slate-900 mb-1">Notas internas</h3><p class="text-xs text-slate-500 mb-3">Visíveis só para administradores</p>
          <form id="form-nota" class="space-y-2"><label class="sr-only" for="nota">Nova nota</label><textarea id="nota" rows="2" maxlength="300" class="campo" placeholder="Ex: liguei para confirmar o endereço"></textarea><button class="btn-sec w-full !py-2 text-xs" type="submit">Adicionar nota</button></form>
          <ul id="notas" class="mt-4 space-y-3 text-sm"></ul></div>
      </div>
    </div>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/admin/admin.js"></script>
<script src="${ctx}/assets/js/admin/detalhe-utilizador.js"></script>
</body>

</html>
