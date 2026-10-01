<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Financeiro - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <link rel="stylesheet" href="${ctx}/assets/css/admin/admin.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="financeiro" data-titulo="Auditoria Financeira e Caixa OngSave" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">

  <div class="flex gap-3 items-start rounded-2xl border border-violet-200 bg-violet-50 text-violet-900 px-5 py-4 text-sm">
    <i class="fa-solid fa-circle-info mt-0.5"></i>
    <p>Os valores abaixo vêm das faturas e dos fretes registados na plataforma. A liquidação dos pagamentos deve ser feita por um <b>provedor de pagamento autorizado</b> (com divisão de pagamento e custódia), e não por uma conta própria da plataforma. Confirme esse desenho com o jurídico antes de operar.</p>
  </div>

  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
    <div class="card p-6"><div class="w-12 h-12 rounded-2xl bg-violet-50 text-violet-600 grid place-items-center text-xl mb-4"><i class="fa-solid fa-arrow-trend-up"></i></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Receita do mês</p><h3 id="k-rec" class="skel text-2xl font-extrabold mt-1">R$ 0</h3><p id="k-rec-sub" class="text-xs text-slate-500 mt-1">--</p></div>
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 grid place-items-center text-xl"><i class="fa-solid fa-lock"></i></div><span class="text-xs font-bold text-slate-500 bg-slate-100 px-2.5 py-1 rounded-full">Intocável</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Conta reserva de liquidez</p><h3 id="k-res" class="skel text-2xl font-extrabold mt-1">R$ 0</h3><p id="k-res-sub" class="text-xs text-slate-500 mt-1">--</p></div>
    <div class="card p-6"><div class="flex justify-between items-start mb-4"><div class="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 grid place-items-center text-xl"><i class="fa-solid fa-shield-halved"></i></div><span id="k-cob-st" class="text-xs font-bold px-2.5 py-1 rounded-full border">--</span></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Cobertura de fretes</p><h3 id="k-cob" class="skel text-2xl font-extrabold mt-1">0 meses</h3><p class="text-xs text-slate-500 mt-1">Meta: 3 meses de fretes</p></div>
    <div class="card p-6"><div class="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 grid place-items-center text-xl mb-4"><i class="fa-solid fa-truck-ramp-box"></i></div>
      <p class="text-xs font-bold uppercase tracking-wider text-slate-400">Fretes pagos no mês</p><h3 id="k-fre" class="skel text-2xl font-extrabold mt-1">R$ 0</h3><p id="k-fre-sub" class="text-xs text-slate-500 mt-1">--</p></div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2">
      <div class="flex flex-wrap justify-between items-center gap-2 mb-2"><h3 class="text-base font-bold text-slate-900"><i class="fa-solid fa-lock text-violet-600 mr-1"></i> Buffer de liquidez</h3><p id="res-status" class="text-sm font-semibold"></p></div>
      <p class="text-xs text-slate-500 mb-4"><span id="res-pct"></span>% de toda receita é separado para a reserva. Ela só pode pagar fretes se contratos corporativos caírem. Não há retirada nesta tela.</p>
      <div class="h-4 bg-slate-100 rounded-full overflow-hidden"><div id="res-barra" class="h-full rounded-full bg-violet-500 transition-all duration-700" style="width:0"></div></div>
      <div class="flex justify-between text-xs text-slate-500 mt-2"><span id="res-esq"></span><span id="res-dir"></span></div>
      <p id="res-nota" class="mt-4 text-sm text-slate-600"></p>
    </div>
    <div class="card p-6">
      <h3 class="text-base font-bold text-slate-900">Simulador da reserva</h3><p class="text-xs text-slate-500 mb-4">Quanto tempo até a reserva cobrir 3 meses de fretes?</p>
      <div class="space-y-3">
        <div><label class="rotulo" for="sim-rec">Receita mensal (R$)</label><input id="sim-rec" type="number" min="1" class="campo"></div>
        <div><label class="rotulo" for="sim-pct">Fretes como % da receita</label><input id="sim-pct" type="number" min="1" max="200" class="campo"></div>
      </div>
      <p id="sim-out" class="mt-4 text-sm rounded-2xl bg-violet-50 text-violet-900 p-4"></p>
    </div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="card p-6 lg:col-span-2"><h3 class="text-base font-bold text-slate-900">Entradas, fretes e reserva</h3><p class="text-xs text-slate-500 mb-4">Últimos 6 meses</p><div class="h-72"><canvas id="finChart"></canvas></div></div>
    <div class="card p-6"><h3 class="text-base font-bold text-slate-900">Receita por plano</h3><p class="text-xs text-slate-500 mb-4">Empresas ativas</p><div id="planos" class="space-y-4"></div></div>
  </div>

  <form id="form-param" class="card p-6 md:p-8 space-y-6" novalidate>
    <div><h3 class="text-base font-bold text-slate-900"><i class="fa-solid fa-sliders text-violet-600 mr-1"></i> Parâmetros da plataforma</h3><p class="text-xs text-slate-500">Valem para todos os perfis. Os valores iniciais são exemplos: ajuste com os números do piloto.</p></div>
    <div class="grid sm:grid-cols-3 lg:grid-cols-6 gap-4">
      <div><label class="rotulo" for="p-raio">Raio máximo (km)</label><input id="p-raio" type="number" min="1" class="campo"></div>
      <div><label class="rotulo" for="p-res">Reserva (% da receita)</label><input id="p-res" type="number" min="1" max="100" class="campo"></div>
      <div><label class="rotulo" for="f-carro">Frete carro (R$)</label><input id="f-carro" type="number" min="1" class="campo"></div>
      <div><label class="rotulo" for="f-camionete">Frete camionete (R$)</label><input id="f-camionete" type="number" min="1" class="campo"></div>
      <div><label class="rotulo" for="f-van">Frete van (R$)</label><input id="f-van" type="number" min="1" class="campo"></div>
      <div><label class="rotulo" for="f-caminhao">Frete caminhão (R$)</label><input id="f-caminhao" type="number" min="1" class="campo"></div>
    </div>
    <div class="overflow-x-auto"><table class="w-full text-left text-sm"><thead><tr class="text-[11px] font-bold uppercase tracking-wider text-slate-400"><th class="py-2 pr-4">Plano</th><th class="py-2 pr-4">Preço mensal (R$)</th><th class="py-2 pr-4">Franquia (lotes/mês)</th><th class="py-2">Taxa por lote extra (R$)</th></tr></thead><tbody id="param-planos"></tbody></table></div>
    <div class="grid sm:grid-cols-3 gap-4">
      <div><label class="rotulo" for="m-co2">CO₂ por kg salvo (kg)</label><input id="m-co2" type="number" step="0.01" min="0" class="campo"></div>
      <div><label class="rotulo" for="m-ref">Refeições por kg</label><input id="m-ref" type="number" step="0.1" min="0" class="campo"></div>
      <div><label class="rotulo" for="m-fonte">Fonte da metodologia</label><input id="m-fonte" maxlength="200" class="campo" placeholder="Ex: órgão, estudo e ano"></div>
    </div>
    <p class="text-xs text-amber-700 bg-amber-50 rounded-xl px-4 py-3"><i class="fa-solid fa-triangle-exclamation mr-1"></i> Enquanto a fonte da metodologia estiver vazia, o relatório ESG das empresas aparece marcado como estimativa.</p>
    <div class="flex justify-end"><button class="btn-lar" type="submit">Salvar parâmetros</button></div>
  </form>

  <div class="card overflow-hidden">
    <div class="p-6 border-b border-slate-200 space-y-4">
      <div class="flex flex-wrap justify-between items-center gap-3"><div><h3 class="text-base font-bold text-slate-900">Movimentações</h3><p class="text-xs text-slate-500">Entradas, fretes, taxas e aportes à reserva</p></div>
        <div class="flex flex-wrap gap-2"><div class="relative"><i class="fa-solid fa-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs"></i>
          <input id="busca" type="search" placeholder="Buscar descrição" aria-label="Buscar movimentações" class="pl-8 pr-3 py-2 w-56 max-w-full bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-violet-300"></div>
          <button id="btn-csv" class="btn-sec !py-2"><i class="fa-solid fa-file-csv mr-1"></i> CSV</button></div></div>
      <div id="chips" class="flex flex-wrap gap-2" role="group" aria-label="Filtrar movimentações"></div>
    </div>
    <div class="overflow-x-auto"><table class="w-full text-left border-collapse"><thead><tr class="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-200"><th class="py-4 px-6">Data</th><th class="py-4 px-6">Descrição</th><th class="py-4 px-6">Tipo</th><th class="py-4 px-6 text-right">Valor</th><th class="py-4 px-6">Situação</th></tr></thead><tbody id="linhas" class="divide-y divide-slate-100 text-sm"></tbody></table>
      <p id="vazio" class="hidden text-center text-sm text-slate-500 py-10">Nenhuma movimentação encontrada.</p></div>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/admin/admin.js"></script>
<script src="${ctx}/assets/js/admin/financeiro.js"></script>
</body>

</html>
