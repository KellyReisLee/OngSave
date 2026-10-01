<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Gerir Utilizadores - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/admin/admin.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="usuarios" data-titulo="Gerir Utilizadores" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">

  <div id="abas" class="flex flex-wrap gap-2" role="tablist" aria-label="Tipo de utilizador"></div>

  <div class="card overflow-hidden">
    <div class="p-6 border-b border-slate-200 space-y-4">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div><h2 id="titulo-lista" class="text-base font-bold text-slate-900"></h2><p class="text-xs text-slate-500">Aprove, suspenda ou bloqueie. Toda ação sensível pede justificativa e fica na auditoria.</p></div>
        <div class="flex flex-wrap gap-2 items-center">
          <div class="relative"><i class="fa-solid fa-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs"></i>
            <input id="busca" type="search" placeholder="Buscar nome, documento ou e-mail" aria-label="Buscar utilizadores" class="pl-8 pr-3 py-2 w-64 max-w-full bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-violet-300"></div>
          <label class="sr-only" for="ordem">Ordenar</label>
          <select id="ordem" class="bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs text-slate-600 font-semibold focus:outline-none">
            <option value="recentes">Mais recentes</option><option value="risco">Maior risco</option><option value="nome">Nome (A a Z)</option>
          </select>
          <button id="btn-csv" class="btn-sec !py-2"><i class="fa-solid fa-file-csv mr-1"></i> CSV</button>
        </div>
      </div>
      <div id="chips" class="flex flex-wrap gap-2" role="group" aria-label="Filtrar por situação"></div>
      <div id="lote" class="hidden flex flex-wrap items-center gap-3 rounded-2xl bg-violet-50 border border-violet-200 px-4 py-3 text-sm">
        <b id="lote-n" class="text-violet-800"></b>
        <button id="lote-aprovar" class="btn-lar !py-2 !px-4 text-xs">Aprovar elegíveis</button>
        <button id="lote-suspender" class="btn-sec !py-2 !px-4 text-xs">Suspender</button>
        <button id="lote-limpar" class="ml-auto text-xs font-semibold text-slate-500 hover:text-slate-800">Limpar seleção</button>
      </div>
    </div>

    <div class="overflow-x-auto">
      <table class="w-full text-left border-collapse">
        <thead><tr class="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-200">
          <th class="py-4 px-4 w-10"><input id="todos" type="checkbox" class="accent-violet-600 w-4 h-4" aria-label="Selecionar todos da página"></th>
          <th class="py-4 px-4">Utilizador</th><th class="py-4 px-4">Contato</th><th id="col-det" class="py-4 px-4">Detalhe</th><th class="py-4 px-4">Situação</th><th class="py-4 px-4">Risco</th><th class="py-4 px-4">Cadastro</th><th class="py-4 px-4 text-right">Ações</th></tr></thead>
        <tbody id="linhas" class="divide-y divide-slate-100 text-sm"></tbody>
      </table>
      <p id="vazio" class="hidden text-center text-sm text-slate-500 py-10">Nenhum utilizador encontrado com esses filtros.</p>
    </div>

    <div class="p-4 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500">
      <p id="pag-info"></p>
      <div class="flex gap-2"><button id="pag-ant" class="btn-sec !py-2 !px-4 text-xs">Anterior</button><button id="pag-prox" class="btn-sec !py-2 !px-4 text-xs">Próxima</button></div>
    </div>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/admin/admin.js"></script>
<script src="${ctx}/assets/js/admin/gerir-utilizadores.js"></script>
</body>

</html>
