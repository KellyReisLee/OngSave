<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Demonstrativo - OngSave Brasil</title>
  <link rel="icon" type="image/png" href="${ctx}/assets/images/favicon.jpeg">
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    body { font-family: 'Inter', sans-serif; }
    .cartao { transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease; }
    .cartao:hover { transform: translateY(-4px); }
    .cartao:hover .seta { transform: translateX(4px); }
    .seta { transition: transform .25s ease; }
    .pulso { animation: pulso 1.8s ease-in-out infinite; }
    @keyframes pulso { 0%, 100% { opacity: 1; } 50% { opacity: .35; } }
  </style>
</head>

<body class="bg-slate-950 min-h-screen text-white overflow-x-hidden">

  <!-- Fundo -->
  <div class="fixed inset-0 -z-10">
    <img src="https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=1600&q=70" alt=""
      class="absolute inset-0 w-full h-full object-cover opacity-20">
    <div class="absolute inset-0 bg-gradient-to-br from-slate-950 via-slate-900/95 to-orange-950/70"></div>
  </div>

  <!-- Topo -->
  <header class="max-w-6xl mx-auto px-4 sm:px-6 pt-6 flex items-center justify-between gap-4">
    <a href="${ctx}/" class="flex items-center gap-2.5 group">
      <div class="w-10 h-10 rounded-xl flex items-center justify-center text-white shadow-lg shadow-orange-500/30 group-hover:scale-105 transition-transform"
        style="background: linear-gradient(135deg, #f97316, #ea580c);">
        <i class="fa-solid fa-leaf text-sm"></i>
      </div>
      <span class="text-xl font-extrabold tracking-tight">Ong<span style="color:#fb923c;">Save</span></span>
    </a>
    <a href="${ctx}/" class="text-sm font-semibold text-slate-300 hover:text-orange-400 transition-colors">
      <i class="fa-solid fa-arrow-left mr-1"></i> Voltar ao site
    </a>
  </header>

  <main class="max-w-6xl mx-auto px-4 sm:px-6 py-12 sm:py-16">

    <!-- Introdução -->
    <div class="max-w-3xl">
      <c:choose>
        <c:when test="${simAtivo}">
          <span class="inline-flex items-center gap-2 py-1.5 px-3 rounded-full bg-emerald-500/10 text-emerald-300 text-xs font-bold tracking-wide uppercase border border-emerald-400/30">
            <span class="w-2 h-2 rounded-full bg-emerald-400 pulso"></span> Simulação ao vivo ligada
          </span>
        </c:when>
        <c:otherwise>
          <span class="inline-flex items-center gap-2 py-1.5 px-3 rounded-full bg-amber-500/10 text-amber-300 text-xs font-bold tracking-wide uppercase border border-amber-400/30">
            <i class="fa-solid fa-triangle-exclamation"></i> Simulação desligada
          </span>
        </c:otherwise>
      </c:choose>

      <h1 class="mt-5 text-4xl sm:text-5xl font-extrabold tracking-tight leading-tight">
        Veja a OngSave <span class="text-transparent bg-clip-text bg-gradient-to-r from-orange-400 to-amber-300">por dentro</span>.
      </h1>
      <p class="mt-5 text-slate-300 text-base sm:text-lg leading-relaxed font-light">
        Escolha um perfil e entre direto no painel, sem senha. Os dados vêm do banco real e,
        com a simulação ligada, uma pequena frota de motoristas publica, recolhe e entrega doações sozinha:
        os veículos andam no mapa, os KPIs e a carteira mudam a cada poucos segundos.
      </p>
    </div>

    <c:if test="${not empty erro}">
      <div class="mt-8 p-4 rounded-2xl bg-red-500/10 border border-red-400/30 text-sm text-red-200 font-medium max-w-3xl" role="alert">
        <i class="fa-solid fa-circle-exclamation mr-1"></i> <c:out value="${erro}"/>
      </div>
    </c:if>
    <c:if test="${not simAtivo}">
      <div class="mt-8 p-4 rounded-2xl bg-amber-500/10 border border-amber-400/30 text-sm text-amber-100 max-w-3xl">
        Os painéis abrem, mas os veículos não andam sozinhos. Para ligar a simulação, defina
        <code class="px-1.5 py-0.5 rounded bg-black/30">app.demo=true</code> no <code class="px-1.5 py-0.5 rounded bg-black/30">ongsave.properties</code>
        e reinicie o servidor.
      </div>
    </c:if>

    <!-- Cartões dos perfis -->
    <div class="mt-12 grid grid-cols-1 md:grid-cols-2 gap-5">

      <a href="${ctx}/demo/empresa" class="cartao group block p-6 sm:p-7 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-md hover:border-orange-400/60 hover:shadow-2xl hover:shadow-orange-500/10">
        <div class="flex items-start justify-between gap-4">
          <div class="w-12 h-12 rounded-2xl bg-orange-500/15 text-orange-400 flex items-center justify-center text-xl border border-orange-400/20">
            <i class="fa-solid fa-store"></i>
          </div>
          <span class="text-[11px] font-semibold text-slate-400 bg-black/20 rounded-full px-2.5 py-1">empresa@ongsave.com</span>
        </div>
        <h2 class="mt-5 text-xl font-extrabold">Empresa doadora</h2>
        <p class="text-sm text-slate-400 mt-1">Supermercado Bom Preço</p>
        <ul class="mt-4 space-y-2 text-sm text-slate-300">
          <li><i class="fa-solid fa-check text-orange-400 mr-2"></i>Publicar um lote de excedentes com foto</li>
          <li><i class="fa-solid fa-check text-orange-400 mr-2"></i>Registar a retirada e gerar o código para o motorista</li>
          <li><i class="fa-solid fa-check text-orange-400 mr-2"></i>Acompanhar o veículo no mapa e o relatório ESG</li>
        </ul>
        <span class="mt-6 inline-flex items-center gap-2 text-sm font-bold text-orange-400">
          Abrir painel da empresa <i class="seta fa-solid fa-arrow-right"></i>
        </span>
      </a>

      <a href="${ctx}/demo/ong" class="cartao group block p-6 sm:p-7 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-md hover:border-emerald-400/60 hover:shadow-2xl hover:shadow-emerald-500/10">
        <div class="flex items-start justify-between gap-4">
          <div class="w-12 h-12 rounded-2xl bg-emerald-500/15 text-emerald-400 flex items-center justify-center text-xl border border-emerald-400/20">
            <i class="fa-solid fa-hand-holding-heart"></i>
          </div>
          <span class="text-[11px] font-semibold text-slate-400 bg-black/20 rounded-full px-2.5 py-1">ong@ongsave.com</span>
        </div>
        <h2 class="mt-5 text-xl font-extrabold">ONG</h2>
        <p class="text-sm text-slate-400 mt-1">Casa Esperança</p>
        <ul class="mt-4 space-y-2 text-sm text-slate-300">
          <li><i class="fa-solid fa-check text-emerald-400 mr-2"></i>Aceitar ou recusar as propostas de doação</li>
          <li><i class="fa-solid fa-check text-emerald-400 mr-2"></i>Ver o motorista a chegar e gerar o token de entrega</li>
          <li><i class="fa-solid fa-check text-emerald-400 mr-2"></i>Conferir peso e condição do que foi recebido</li>
        </ul>
        <span class="mt-6 inline-flex items-center gap-2 text-sm font-bold text-emerald-400">
          Abrir painel da ONG <i class="seta fa-solid fa-arrow-right"></i>
        </span>
      </a>

      <a href="${ctx}/demo/motorista" class="cartao group block p-6 sm:p-7 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-md hover:border-sky-400/60 hover:shadow-2xl hover:shadow-sky-500/10">
        <div class="flex items-start justify-between gap-4">
          <div class="w-12 h-12 rounded-2xl bg-sky-500/15 text-sky-400 flex items-center justify-center text-xl border border-sky-400/20">
            <i class="fa-solid fa-truck-fast"></i>
          </div>
          <span class="text-[11px] font-semibold text-slate-400 bg-black/20 rounded-full px-2.5 py-1">motorista@ongsave.com</span>
        </div>
        <h2 class="mt-5 text-xl font-extrabold">Motorista parceiro</h2>
        <p class="text-sm text-slate-400 mt-1">João da Silva · Van</p>
        <ul class="mt-4 space-y-2 text-sm text-slate-300">
          <li><i class="fa-solid fa-check text-sky-400 mr-2"></i>Aceitar uma entrega disponível no raio</li>
          <li><i class="fa-solid fa-check text-sky-400 mr-2"></i>Rota Ativa com o botão <b>Simular trajeto</b></li>
          <li><i class="fa-solid fa-check text-sky-400 mr-2"></i>Validar com token e foto, e ver o frete na carteira</li>
        </ul>
        <span class="mt-6 inline-flex items-center gap-2 text-sm font-bold text-sky-400">
          Abrir painel do motorista <i class="seta fa-solid fa-arrow-right"></i>
        </span>
      </a>

      <a href="${ctx}/demo/admin" class="cartao group block p-6 sm:p-7 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-md hover:border-violet-400/60 hover:shadow-2xl hover:shadow-violet-500/10">
        <div class="flex items-start justify-between gap-4">
          <div class="w-12 h-12 rounded-2xl bg-violet-500/15 text-violet-300 flex items-center justify-center text-xl border border-violet-400/20">
            <i class="fa-solid fa-shield-halved"></i>
          </div>
          <span class="text-[11px] font-semibold text-slate-400 bg-black/20 rounded-full px-2.5 py-1">admin@ongsave.com</span>
        </div>
        <h2 class="mt-5 text-xl font-extrabold">Administrador</h2>
        <p class="text-sm text-slate-400 mt-1">Equipa OngSave</p>
        <ul class="mt-4 space-y-2 text-sm text-slate-300">
          <li><i class="fa-solid fa-check text-violet-300 mr-2"></i>Mapa ao vivo com todas as entregas em curso</li>
          <li><i class="fa-solid fa-check text-violet-300 mr-2"></i>Aprovar cadastros e analisar documentos</li>
          <li><i class="fa-solid fa-check text-violet-300 mr-2"></i>Resolver ocorrências e acompanhar o financeiro</li>
        </ul>
        <span class="mt-6 inline-flex items-center gap-2 text-sm font-bold text-violet-300">
          Abrir painel do administrador <i class="seta fa-solid fa-arrow-right"></i>
        </span>
      </a>
    </div>

    <!-- Dicas e roteiro -->
    <div class="mt-12 grid grid-cols-1 lg:grid-cols-3 gap-5">
      <div class="p-6 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-md">
        <div class="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center text-orange-300 mb-4">
          <i class="fa-regular fa-window-restore"></i>
        </div>
        <h3 class="font-bold">Abra vários perfis de uma vez</h3>
        <p class="mt-2 text-sm text-slate-400 leading-relaxed">
          Clique nos cartões com o botão do meio do rato (ou Cmd/Ctrl + clique) para abrir cada perfil numa aba.
          Todos ficam ativos ao mesmo tempo no mesmo navegador.
        </p>
      </div>

      <div class="lg:col-span-2 p-6 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-md">
        <h3 class="font-bold"><i class="fa-solid fa-route text-orange-400 mr-2"></i>Roteiro de uma doação completa</h3>
        <ol class="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3 text-sm text-slate-300">
          <li><b class="text-orange-400 mr-1">1.</b> <b>Empresa</b> › Nova doação › publique um lote.</li>
          <li><b class="text-orange-400 mr-1">2.</b> <b>ONG</b> › aceite a proposta.</li>
          <li><b class="text-orange-400 mr-1">3.</b> <b>Motorista</b> › Entregas › aceite › Rota Ativa › Simular trajeto.</li>
          <li><b class="text-orange-400 mr-1">4.</b> <b>Empresa</b> › registe a retirada; o motorista digita o código.</li>
          <li><b class="text-orange-400 mr-1">5.</b> <b>ONG</b> › gere o token; o motorista valida com token + foto de demonstração.</li>
          <li><b class="text-orange-400 mr-1">6.</b> <b>ONG</b> › confira o recebimento: o frete é liberado na carteira.</li>
        </ol>
      </div>
    </div>

    <p class="mt-10 text-xs text-slate-500 max-w-3xl leading-relaxed">
      Ambiente de demonstração: as contas, empresas e ONGs são fictícias e os movimentos dos veículos são simulados.
      Pode clicar à vontade. Se uma conta for suspensa durante os testes, ela é reativada ao entrar de novo por esta página.
    </p>
  </main>

  <footer class="max-w-6xl mx-auto px-4 sm:px-6 pb-8 text-center text-xs text-slate-500">
    &copy; 2026 OngSave Brasil. Logística Inteligente B2B.
  </footer>
</body>

</html>
