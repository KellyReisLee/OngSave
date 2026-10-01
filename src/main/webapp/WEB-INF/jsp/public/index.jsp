<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>OngSave - Logística B2B de Redistribuição de Alimentos</title>
  <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.jpeg">
  <!-- Google Fonts Inter -->
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap"
    rel="stylesheet">
  <!-- FontAwesome para ícones -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <!-- Folha de Estilos -->
  <link rel="stylesheet" href="${ctx}/assets/css/index.css">
  <script src="https://cdn.tailwindcss.com"></script>
</head>

<body class="bg-slate-50 text-slate-900">

  <!-- Navbar -->
  <header id="navbar" class="fixed top-0 left-0 z-50 w-full transition-all duration-500 bg-transparent py-5">
    <nav class="mx-auto flex max-w-7xl items-center justify-between px-6">
      <!-- Logo -->
      <a href="#" class="group flex items-center gap-2.5 text-decoration-none">
        <div
          class="relative flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 shadow-lg shadow-orange-500/30 transition-transform duration-300 group-hover:scale-110"
          style="background: linear-gradient(135deg, #f97316, #ea580c);">
          <i class="fa-solid fa-leaf text-white text-sm"></i>
        </div>
        <span class="text-xl font-bold tracking-tight text-slate-900">
          Ong<span class="text-orange-600" style="color: #ea580c;">Save</span>
        </span>
      </a>

      <!-- Desktop Nav -->
      <div class="hidden items-center gap-8 lg:flex">
        <a href="#home"
          class="text-sm font-medium text-slate-600 transition-colors duration-300 hover:text-orange-600">Plataforma</a>
        <a href="#impact"
          class="text-sm font-medium text-slate-600 transition-colors duration-300 hover:text-orange-600">Impacto
          ESG</a>
        <a href="#como-funciona"
          class="text-sm font-medium text-slate-600 transition-colors duration-300 hover:text-orange-600">Como
          Funciona</a>
        <a href="#partners"
          class="text-sm font-medium text-slate-600 transition-colors duration-300 hover:text-orange-600">Parceiros</a>
      </div>

      <!-- CTA Buttons -->
      <div class="hidden items-center gap-3 lg:flex">
        <a href="${ctx}/login" class="inline-block text-decoration-none">
          <button
            class="flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-700 transition-all duration-300 hover:bg-slate-100 border-0 bg-transparent cursor-pointer">
            <i class="fa-solid fa-arrow-right-to-bracket"></i> Entrar
          </button>
        </a>
        <a href="${ctx}/cadastro" class="inline-block text-decoration-none">
          <button
            class="flex items-center gap-2 rounded-lg bg-gradient-to-r from-orange-500 to-orange-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-orange-500/25 transition-all duration-300 hover:scale-105 border-0 cursor-pointer"
            style="background: linear-gradient(135deg, #f97316, #ea580c);">
            <i class="fa-solid fa-user-plus"></i> Criar Conta
          </button>
        </a>
      </div>

      <!-- Mobile Toggle -->
      <button id="mobile-toggle" class="text-slate-700 lg:hidden bg-transparent border-0 cursor-pointer text-xl">
        <i class="fa-solid fa-bars" id="menu-icon"></i>
      </button>
    </nav>

    <!-- Mobile Menu -->
    <div id="mobile-menu"
      class="hidden mt-4 border-t border-slate-200 bg-white/95 backdrop-blur-xl lg:hidden px-6 py-4 shadow-lg">
      <div class="flex flex-col gap-2">
        <a href="#features"
          class="mobile-link rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100">Plataforma</a>
        <a href="#impact"
          class="mobile-link rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100">Impacto ESG</a>
        <a href="#how"
          class="mobile-link rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100">Como
          Funciona</a>
        <a href="#partners"
          class="mobile-link rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100">Parceiros</a>
        <div class="mt-3 flex flex-col gap-2 pt-2 border-t border-slate-100">
          <button
            class="flex items-center justify-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 bg-white">
            <i class="fa-solid fa-arrow-right-to-bracket"></i> Entrar
          </button>

          <button
            class="flex items-center justify-center gap-2 rounded-lg px-4 py-3 text-sm font-semibold text-white border-0"
            style="background: linear-gradient(135deg, #f97316, #ea580c);">
            <i class="fa-solid fa-user-plus"></i> Criar Conta
          </button>
          </a>
        </div>
      </div>
    </div>
  </header>

  <main>
    <!-- Hero Section -->
    <section id="home" class="relative flex min-h-screen items-center overflow-hidden pt-28 pb-16">
      <div class="grid-pattern absolute inset-0 opacity-70 pointer-events-none"></div>
      <div class="glow-orb top-[-10%] left-[-5%] h-96 w-96 bg-orange-300/30 glow-orb-1"></div>
      <div class="glow-orb bottom-[-10%] right-[-5%] h-[28rem] w-[28rem] bg-emerald-300/25 glow-orb-2"></div>

      <div class="relative z-10 mx-auto grid max-w-7xl grid-cols-1 items-center gap-12 px-6 lg:grid-cols-2 w-full">
        <!-- Left: Copy -->
        <div>
          <div
            class="animate-fade-in-up inline-flex items-center gap-2 rounded-full border border-orange-200 bg-orange-50 px-4 py-2 text-xs font-semibold text-orange-600"
            style="background-color: #fff7ed; border-color: #fed7aa; color: #ea580c; animation-delay: 0.1s;">
            <i class="fa-solid fa-shield-halved"></i> Plataforma ESG B2B · Logística Inteligente
          </div>

          <h1
            class="animate-fade-in-up mt-6 text-4xl font-extrabold leading-[1.15] tracking-tight text-slate-900 sm:text-5xl lg:text-6xl"
            style="animation-delay: 0.2s;">
            Combate ao Desperdício com <span class="text-shimmer">Logística Inteligente</span>
          </h1>

          <p class="animate-fade-in-up mt-6 max-w-xl text-lg leading-relaxed text-slate-500"
            style="animation-delay: 0.35s;">
            A plataforma B2B que conecta empresas doadoras a ONGs em um raio geofenciado de <span
              class="font-semibold text-orange-600" style="color: #ea580c;">15 km</span>. Reduza o desperdício, gere
            relatórios ESG automatizados e amplie seu impacto socioambiental com rastreabilidade total.
          </p>

          <!-- CTA -->
          <div class="animate-fade-in-up mt-8 flex flex-col items-start gap-4 sm:flex-row sm:items-center"
            style="animation-delay: 0.5s;">
            <a href="${ctx}/cadastro" class="inline-block text-decoration-none">
              <button
                class="group flex items-center gap-2 rounded-xl px-7 py-4 text-base font-bold text-white shadow-xl transition-all duration-300 hover:scale-105 border-0 cursor-pointer"
                style="background: linear-gradient(135deg, #f97316, #ea580c); box-shadow: 0 10px 25px -5px rgba(249, 115, 22, 0.4);">
                Começar Agora <i class="fa-solid fa-arrow-right transition-transform group-hover:translate-x-1"></i>
              </button>
            </a>


            <button
              class="rounded-xl border border-slate-200 bg-white px-7 py-4 text-base font-semibold text-slate-700 transition-all duration-300 hover:border-orange-300 hover:text-orange-600 hover:shadow-md cursor-pointer">
              Ver Demonstração
            </button>
          </div>

          <!-- Trust badges -->
          <div class="animate-fade-in-up mt-10 flex flex-wrap items-center gap-x-8 gap-y-3"
            style="animation-delay: 0.65s;">
            <div class="flex items-center gap-2 text-sm text-slate-500">
              <div class="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></div>
              +1.200 ONGs ativas
            </div>
            <div class="flex items-center gap-2 text-sm text-slate-500">
              <div class="h-2 w-2 rounded-full bg-orange-500 animate-pulse" style="background-color: #f97316;"></div>
              3.4M kg salvos
            </div>
            <div class="flex items-center gap-2 text-sm text-slate-500">
              <div class="h-2 w-2 rounded-full bg-blue-500 animate-pulse"></div>
              Verificação anti-fraude
            </div>
          </div>
        </div>

        <!-- Right: Floating interactive alert card -->
        <div class="animate-fade-in-right relative flex justify-center" style="animation-delay: 0.4s;">
          <div class="animate-float relative w-full max-w-md">
            <!-- Main card -->
            <div class="animate-glow rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
              <!-- Header -->
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div class="relative flex h-10 w-10 items-center justify-center rounded-xl bg-orange-100"
                    style="background-color: #ffedd5;">
                    <i class="fa-solid fa-truck-fast text-orange-600" style="color: #ea580c;"></i>
                  </div>
                  <div>
                    <p class="text-sm font-semibold text-slate-900 m-0">Alerta de Coleta</p>
                    <p class="text-xs text-slate-500 m-0">Tempo real · #OS-2847</p>
                  </div>
                </div>
                <div class="relative flex items-center gap-1">
                  <span
                    class="inline-flex items-center gap-1.5 rounded-full bg-red-50 px-3 py-1.5 text-xs font-bold text-red-500 ring-1 ring-red-200"
                    style="background-color: #fef2f2; color: #ef4444;">
                    <i class="fa-solid fa-bolt text-xs"></i> ALTA PRIORIDADE
                  </span>
                  <span class="text-xs font-bold text-red-500">&lt; 24h</span>
                </div>
              </div>

              <!-- Divider -->
              <div class="my-5 h-px bg-gradient-to-r from-transparent via-slate-200 to-transparent"></div>

              <!-- Metrics grid -->
              <div class="grid grid-cols-3 gap-3">
                <div class="rounded-xl bg-slate-50 p-3.5 text-center border border-slate-100">
                  <i class="fa-solid fa-box-archive text-orange-600 text-lg" style="color: #ea580c;"></i>
                  <p class="mt-2 text-xl font-bold text-slate-900 m-0">247<span class="text-sm text-slate-500">
                      kg</span></p>
                  <p class="text-[10px] uppercase tracking-wide text-slate-400 m-0 mt-1">Peso</p>
                </div>
                <div class="rounded-xl bg-slate-50 p-3.5 text-center border border-slate-100">
                  <i class="fa-solid fa-location-dot text-emerald-500 text-lg"></i>
                  <p class="mt-2 text-xl font-bold text-slate-900 m-0">15<span class="text-sm text-slate-500"> km</span>
                  </p>
                  <p class="text-[10px] uppercase tracking-wide text-slate-400 m-0 mt-1">Raio</p>
                </div>
                <div class="rounded-xl bg-slate-50 p-3.5 text-center border border-slate-100">
                  <i class="fa-solid fa-chart-line text-emerald-500 text-lg"></i>
                  <p class="mt-2 text-xl font-bold text-slate-900 m-0">89<span class="text-sm text-slate-500"> kg</span>
                  </p>
                  <p class="text-[10px] uppercase tracking-wide text-slate-400 m-0 mt-1">CO₂ evitado</p>
                </div>
              </div>

              <!-- Progress bar -->
              <div class="mt-5">
                <div class="flex items-center justify-between text-xs mb-2">
                  <span class="text-slate-500">Progresso da rota</span>
                  <span id="progress-text" class="font-semibold text-orange-600" style="color: #ea580c;">78%</span>
                </div>
                <div class="h-2 overflow-hidden rounded-full bg-slate-100">
                  <div id="progress-bar"
                    class="h-full rounded-full bg-gradient-to-r from-orange-500 to-emerald-500 transition-all duration-300"
                    style="width: 78%; background: linear-gradient(90deg, #f97316, #10b981);"></div>
                </div>
              </div>

              <!-- Footer row -->
              <div
                class="mt-5 flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3 border border-slate-100">
                <div class="flex items-center gap-3">
                  <div
                    class="h-8 w-8 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center text-xs font-bold text-white"
                    style="background: #10b981;">
                    GA
                  </div>
                  <div>
                    <p class="text-xs font-semibold text-slate-900 m-0">Green Action NGO</p>
                    <p class="text-[10px] text-slate-400 m-0">Verificada · Token #A7F2</p>
                  </div>
                </div>
                <div class="flex items-center gap-1.5">
                  <i class="fa-solid fa-shield-check text-emerald-500"></i>
                  <span class="text-xs font-semibold text-emerald-600">Dual Check</span>
                </div>
              </div>
            </div>

            <!-- Floating mini badge: top-right -->
            <div
              class="animate-float absolute -top-6 -right-4 rounded-xl border border-emerald-200 bg-white px-4 py-3 shadow-xl"
              style="animation-delay: 1.5s;">
              <div class="flex items-center gap-2.5">
                <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-100"
                  style="background-color: #d1fae5;">
                  <i class="fa-solid fa-camera text-emerald-600 text-xs"></i>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-900 m-0">Foto Verificada</p>
                  <p class="text-[10px] text-slate-500 m-0">Token + Upload</p>
                </div>
              </div>
            </div>

            <!-- Floating mini badge: bottom-left -->
            <div
              class="animate-float absolute -bottom-5 -left-4 rounded-xl border border-orange-200 bg-white px-4 py-3 shadow-xl"
              style="animation-delay: 0.8s;">
              <div class="flex items-center gap-2.5">
                <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-orange-100"
                  style="background-color: #ffedd5;">
                  <i class="fa-solid fa-file-contract text-orange-600 text-xs" style="color: #ea580c;"></i>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-900 m-0">ESG Report</p>
                  <p class="text-[10px] text-slate-500 m-0">Auto-gerado</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div
        class="absolute bottom-0 left-0 h-32 w-full bg-gradient-to-t from-slate-50 to-transparent pointer-events-none">
      </div>
    </section>

    <!-- Features Section -->
    <section id="features" class="relative py-24">
      <div class="mx-auto max-w-7xl px-6">
        <!-- Section header -->
        <div class="reveal mx-auto max-w-2xl text-center">
          <span
            class="inline-block rounded-full border border-orange-200 bg-orange-50 px-4 py-1.5 text-xs font-semibold text-orange-600"
            style="background-color: #fff7ed; border-color: #fed7aa; color: #ea580c;">
            Módulos da Plataforma
          </span>
          <h2 class="mt-4 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl lg:text-5xl">
            Tudo que sua operação precisa
          </h2>
          <p class="mt-4 text-lg text-slate-500">
            Três pilares integrados para uma cadeia de redistribuição transparente, eficiente e auditável.
          </p>
        </div>

        <!-- Cards grid -->
        <div class="mt-16 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          <!-- Card 1 -->
          <div
            class="group reveal relative rounded-2xl border border-slate-200 bg-white p-8 transition-all duration-500 hover:-translate-y-2 hover:shadow-2xl hover:border-orange-300">
            <div
              class="flex h-14 w-14 items-center justify-center rounded-xl bg-orange-100 text-orange-600 transition-transform duration-500 group-hover:scale-110"
              style="background-color: #ffedd5; color: #ea580c;">
              <i class="fa-solid fa-building text-xl"></i>
            </div>
            <h3 class="mt-6 text-xl font-bold text-slate-900">Empresas Doadoras</h3>
            <p class="mt-3 text-sm leading-relaxed text-slate-500">Assinatura B2B com relatórios ESG automatizados,
              rastreabilidade completa de doações e dashboard de impacto em tempo real.</p>
            <ul class="mt-6 space-y-2.5 pl-0 list-none">
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-orange-500" style="background-color: #ea580c;"></div> Relatórios
                ESG automáticos
              </li>
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-orange-500" style="background-color: #ea580c;"></div> Dashboard
                de impacto
              </li>
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-orange-500" style="background-color: #ea580c;"></div> Gestão de
                metas
              </li>
            </ul>
            <div
              class="mt-6 flex items-center gap-1.5 text-sm font-semibold text-slate-400 group-hover:text-orange-600 transition-colors"
              style="transition: color 0.3s;">
              Saber mais <i
                class="fa-solid fa-arrow-up-right text-xs transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5"></i>
            </div>
          </div>

          <!-- Card 2 -->
          <div
            class="group reveal relative rounded-2xl border border-slate-200 bg-white p-8 transition-all duration-500 hover:-translate-y-2 hover:shadow-2xl hover:border-emerald-300">
            <div
              class="flex h-14 w-14 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600 transition-transform duration-500 group-hover:scale-110"
              style="background-color: #d1fae5; color: #10b981;">
              <i class="fa-solid fa-truck text-xl"></i>
            </div>
            <h3 class="mt-6 text-xl font-bold text-slate-900">Motoristas Parceiros</h3>
            <p class="mt-3 text-sm leading-relaxed text-slate-500">Fretes fixos em curto raio com roteirização otimizada
              e carteira digital segura. Receba pagamentos garantidos sem burocracia.</p>
            <ul class="mt-6 space-y-2.5 pl-0 list-none">
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-emerald-500" style="background-color: #10b981;"></div> Frete
                fixo por raio
              </li>
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-emerald-500" style="background-color: #10b981;"></div> Carteira
                digital segura
              </li>
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-emerald-500" style="background-color: #10b981;"></div> Pagamento
                garantido
              </li>
            </ul>
            <div
              class="mt-6 flex items-center gap-1.5 text-sm font-semibold text-slate-400 group-hover:text-emerald-600 transition-colors">
              Saber mais <i
                class="fa-solid fa-arrow-up-right text-xs transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5"></i>
            </div>
          </div>

          <!-- Card 3 -->
          <div
            class="group reveal relative rounded-2xl border border-slate-200 bg-white p-8 transition-all duration-500 hover:-translate-y-2 hover:shadow-2xl hover:border-blue-300">
            <div
              class="flex h-14 w-14 items-center justify-center rounded-xl bg-blue-100 text-blue-600 transition-transform duration-500 group-hover:scale-110"
              style="background-color: #dbeafe; color: #3b82f6;">
              <i class="fa-solid fa-shield-halved text-xl"></i>
            </div>
            <h3 class="mt-6 text-xl font-bold text-slate-900">Segurança Anti-Fraude</h3>
            <p class="mt-3 text-sm leading-relaxed text-slate-500">Sistema de verificação dupla com Token digital da ONG
              e upload obrigatório de foto em cada entrega. Transparência total.</p>
            <ul class="mt-6 space-y-2.5 pl-0 list-none">
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-blue-500" style="background-color: #3b82f6;"></div> Token
                digital da ONG
              </li>
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-blue-500" style="background-color: #3b82f6;"></div> Foto
                obrigatória
              </li>
              <li class="flex items-center gap-2.5 text-sm text-slate-700">
                <div class="h-1.5 w-1.5 rounded-full bg-blue-500" style="background-color: #3b82f6;"></div> Verificação
                dupla
              </li>
            </ul>
            <div
              class="mt-6 flex items-center gap-1.5 text-sm font-semibold text-slate-400 group-hover:text-blue-600 transition-colors">
              Saber mais <i
                class="fa-solid fa-arrow-up-right text-xs transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5"></i>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Seção: Como Funciona o Ciclo do Projeto -->
    <section class="py-24 relative overflow-hidden"
      style="background: linear-gradient(135deg, #b9380a 0%, #7c2d12 100%); width: 100vw; position: relative; left: 50%; right: 50%; margin-left: -50vw; margin-right: -50vw;"
      id="como-funciona">
      <div class="mx-auto max-w-7xl px-6">
        <div class="text-center max-w-3xl mx-auto">
          <span
            class="inline-block rounded-full bg-orange-950/40 px-4 py-1.5 text-xs font-extrabold uppercase tracking-widest text-orange-200 border border-orange-400/30 mb-4 shadow-sm">
            Processo Transparente
          </span>
          <h2 class="text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
            Como Funciona o Ciclo da OngSave
          </h2>
          <p class="mt-4 text-base text-orange-100/90 tracking-wide leading-relaxed">
            Conectamos a tecnologia logística à solidariedade B2B. Descubra o passo a passo de como transformamos
            excedentes corporativos em impacto social e ambiental real.
          </p>
        </div>

        <!-- Grid com cartões inicialmente ocultos -->
        <div class="mt-20 grid grid-cols-1 gap-6 md:grid-cols-4 relative items-center" id="cycle-grid">

          <!-- Passo 1: Registo -->
          <div
            class="cycle-card opacity-0 translate-y-8 transition-all duration-700 ease-out group relative rounded-2xl p-8 border border-orange-300/30 backdrop-blur-md hover:bg-[#7c2d12] hover:border-orange-200 hover:-translate-y-3 hover:shadow-2xl cursor-pointer flex flex-col justify-between"
            style="background-color: rgba(124, 45, 18, 0.45); transition-delay: 100ms;">
            <div>
              <div
                class="absolute -top-4 left-8 flex h-10 w-10 items-center justify-center rounded-xl bg-white text-[#9a3412] font-extrabold text-sm shadow-md border border-orange-200">
                01
              </div>
              <div
                class="mt-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-950/60 text-orange-200 transition-all duration-300 group-hover:bg-orange-600 group-hover:text-white shadow-inner">
                <i class="fa-solid fa-boxes-stacked text-2xl"></i>
              </div>
              <h3 class="mt-6 text-lg font-bold text-white tracking-wide">
                1. Registo de Excedentes
              </h3>
              <p class="mt-3 text-sm text-orange-100/90 tracking-wide leading-relaxed">
                Empresas doadoras e supermercados registam excedentes alimentares próximos do fim da validade ou com
                stock excedentário.
              </p>
            </div>
          </div>

          <!-- Passo 2: Match Inteligente -->
          <div
            class="cycle-card opacity-0 translate-y-8 transition-all duration-700 ease-out group relative rounded-2xl p-8 border border-orange-300/30 backdrop-blur-md hover:bg-[#7c2d12] hover:border-orange-200 hover:-translate-y-3 hover:shadow-2xl cursor-pointer flex flex-col justify-between"
            style="background-color: rgba(124, 45, 18, 0.45); transition-delay: 300ms;">
            <div>
              <div
                class="absolute -top-4 left-8 flex h-10 w-10 items-center justify-center rounded-xl bg-white text-[#9a3412] font-extrabold text-sm shadow-md border border-orange-200">
                02
              </div>
              <div
                class="mt-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-950/60 text-orange-200 transition-all duration-300 group-hover:bg-orange-600 group-hover:text-white shadow-inner">
                <i class="fa-solid fa-network-wired text-2xl"></i>
              </div>
              <h3 class="mt-6 text-lg font-bold text-white tracking-wide">
                2. Match Logístico
              </h3>
              <p class="mt-3 text-sm text-orange-100/90 tracking-wide leading-relaxed">
                O nosso algoritmo otimiza rotas e conecta automaticamente os alimentos disponíveis à ONG mais próxima
                que precisa deles.
              </p>
            </div>
          </div>

          <!-- Passo 3: Transporte -->
          <div
            class="cycle-card opacity-0 translate-y-8 transition-all duration-700 ease-out group relative rounded-2xl p-8 border border-orange-300/30 backdrop-blur-md hover:bg-[#7c2d12] hover:border-orange-200 hover:-translate-y-3 hover:shadow-2xl cursor-pointer flex flex-col justify-between"
            style="background-color: rgba(124, 45, 18, 0.45); transition-delay: 500ms;">
            <div>
              <div
                class="absolute -top-4 left-8 flex h-10 w-10 items-center justify-center rounded-xl bg-white text-[#9a3412] font-extrabold text-sm shadow-md border border-orange-200">
                03
              </div>
              <div
                class="mt-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-950/60 text-orange-200 transition-all duration-300 group-hover:bg-orange-600 group-hover:text-white shadow-inner">
                <i class="fa-solid fa-truck-ramp-box text-2xl"></i>
              </div>
              <h3 class="mt-6 text-lg font-bold text-white tracking-wide">
                3. Recolha e Transporte
              </h3>
              <p class="mt-3 text-sm text-orange-100/90 tracking-wide leading-relaxed">
                Motoristas parceiros certificados realizam o transporte seguro, preservando a cadeia de frio e
                integridade dos produtos.
              </p>
            </div>
          </div>

          <!-- Passo 4: Impacto ESG -->
          <div
            class="cycle-card opacity-0 translate-y-8 transition-all duration-700 ease-out group relative rounded-2xl p-8 border border-orange-300/30 backdrop-blur-md hover:bg-[#7c2d12] hover:border-orange-200 hover:-translate-y-3 hover:shadow-2xl cursor-pointer flex flex-col justify-between"
            style="background-color: rgba(124, 45, 18, 0.45); transition-delay: 700ms;">
            <div>
              <div
                class="absolute -top-4 left-8 flex h-10 w-10 items-center justify-center rounded-xl bg-white text-[#9a3412] font-extrabold text-sm shadow-md border border-orange-200">
                04
              </div>
              <div
                class="mt-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-950/60 text-orange-200 transition-all duration-300 group-hover:bg-orange-600 group-hover:text-white shadow-inner">
                <i class="fa-solid fa-seedling text-2xl"></i>
              </div>
              <h3 class="mt-6 text-lg font-bold text-white tracking-wide">
                4. Impacto e ESG
              </h3>
              <p class="mt-3 text-sm text-orange-100/90 tracking-wide leading-relaxed">
                Alimentos entregues a quem precisa e emissão automática de relatórios de sustentabilidade e impacto ESG
                para a empresa.
              </p>
            </div>
          </div>

        </div>
      </div>
    </section>



    <!-- Impact Counter Section -->
    <section id="impact" class="relative overflow-hidden bg-white py-24 border-t border-slate-100">
      <div class="grid-pattern absolute inset-0 opacity-40 pointer-events-none"></div>

      <div class="relative z-10 mx-auto max-w-7xl px-6">
        <div class="mx-auto max-w-2xl text-center">
          <span
            class="inline-block rounded-full border border-emerald-200 bg-emerald-50 px-4 py-1.5 text-xs font-semibold text-emerald-600"
            style="background-color: #ecfdf5; border-color: #a7f3d0; color: #10b981;">
            Impacto ESG em Tempo Real
          </span>
          <h2 class="mt-4 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl lg:text-5xl">
            Números que transformam
          </h2>
          <p class="mt-4 text-lg text-slate-500">
            Cada doação registrada na OngSave gera impacto mensurável. Acompanhe nossos indicadores ambientais e
            sociais.
          </p>
        </div>

        <!-- Stats grid -->
        <div id="counter-section" class="mt-14 grid grid-cols-2 gap-4 lg:grid-cols-4">
          <div
            class="group rounded-2xl border border-slate-200 bg-white p-6 text-center transition-all duration-500 hover:-translate-y-1.5 hover:shadow-xl hover:border-slate-300">
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-orange-100 text-orange-600"
              style="background-color: #ffedd5; color: #ea580c;">
              <i class="fa-solid fa-box-archive text-lg"></i>
            </div>
            <p class="mt-4 text-2xl font-extrabold text-slate-900 sm:text-3xl m-0">
              <span id="stat-1" data-target="3420000">0</span> kg
            </p>
            <p class="mt-1.5 text-sm text-slate-500 m-0">Alimento Salvo</p>
          </div>

          <div
            class="group rounded-2xl border border-slate-200 bg-white p-6 text-center transition-all duration-500 hover:-translate-y-1.5 hover:shadow-xl hover:border-slate-300">
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600"
              style="background-color: #d1fae5; color: #10b981;">
              <i class="fa-solid fa-cloud text-lg"></i>
            </div>
            <p class="mt-4 text-2xl font-extrabold text-slate-900 sm:text-3xl m-0">
              <span id="stat-2" data-target="890000">0</span> kg
            </p>
            <p class="mt-1.5 text-sm text-slate-500 m-0">CO₂ Evitado</p>
          </div>

          <div
            class="group rounded-2xl border border-slate-200 bg-white p-6 text-center transition-all duration-500 hover:-translate-y-1.5 hover:shadow-xl hover:border-slate-300">
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100 text-blue-600"
              style="background-color: #dbeafe; color: #3b82f6;">
              <i class="fa-solid fa-truck-fast text-lg"></i>
            </div>
            <p class="mt-4 text-2xl font-extrabold text-slate-900 sm:text-3xl m-0">
              <span id="stat-3" data-target="12847">0</span>
            </p>
            <p class="mt-1.5 text-sm text-slate-500 m-0">Entregas Realizadas</p>
          </div>

          <div
            class="group rounded-2xl border border-slate-200 bg-white p-6 text-center transition-all duration-500 hover:-translate-y-1.5 hover:shadow-xl hover:border-slate-300">
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-orange-100 text-orange-600"
              style="background-color: #ffedd5; color: #ea580c;">
              <i class="fa-solid fa-users text-lg"></i>
            </div>
            <p class="mt-4 text-2xl font-extrabold text-slate-900 sm:text-3xl m-0">
              <span id="stat-4" data-target="285000">0</span>
            </p>
            <p class="mt-1.5 text-sm text-slate-500 m-0">Vidas Impactadas</p>
          </div>
        </div>

        <!-- Ticker / marquee -->
        <div class="mt-14 overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 py-5">
          <div class="ticker-track flex w-max gap-12 whitespace-nowrap">
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-orange-500"></span> 3.4M kg de alimentos salvos</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> 890 ton CO₂ evitado</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-orange-500"></span> +1.200 ONGs conectadas</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> 15 km raio geofenciado</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-orange-500"></span> Verificação anti-fraude dupla</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> Relatórios ESG automatizados</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-orange-500"></span> +285K vidas impactadas</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> 100% rastreabilidade</span>
            <!-- Duplicate for seamless looping -->
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-orange-500"></span> 3.4M kg de alimentos salvos</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> 890 ton CO₂ evitado</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-orange-500"></span> +1.200 ONGs conectadas</span>
            <span class="flex items-center gap-3 text-sm font-semibold text-slate-600"><span
                class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> 15 km raio geofenciado</span>
          </div>
        </div>
      </div>
    </section>


  </main>

  <!-- Footer -->
  <footer id="how" class="relative border-t border-white/20 py-16"
    style="background: linear-gradient(135deg, #c2410c 0%, #9a3412 100%); width: 100vw; position: relative; left: 50%; right: 50%; margin-left: -50vw; margin-right: -50vw;">
    <div class="mx-auto max-w-7xl px-6">
      <div class="grid grid-cols-1 gap-12 md:grid-cols-4" id="partners">
        <div class="md:col-span-2">
          <div class="flex items-center gap-2.5">
            <!-- Logótipo com inversão de cores suave no hover -->
            <div
              class="group/logo relative flex h-9 w-9 items-center justify-center rounded-xl bg-white border-2 border-orange-500 transition-colors duration-300 hover:bg-[#c2410c] hover:border-white cursor-pointer"
              style="background-color: #ffffff; border-color: #f5f2ef;">
              <i class="fa-solid fa-leaf text-sm transition-colors duration-300 group-hover/logo:text-white"
                style="color: #f97316;"></i>
            </div>
            <span class="text-xl font-bold tracking-tight text-white">
              Ong<span class="text-orange-200" style="color: #fed7aa;">Save</span>
            </span>
          </div>
          <p class="mt-4 max-w-sm text-sm leading-relaxed text-white tracking-wide opacity-95">
            Plataforma B2B de redistribuição logística de alimentos com foco em ESG. Conectamos empresas, motoristas e
            ONGs para combater o desperdício com tecnologia e transparência.
          </p>
          <div class="mt-6 flex gap-3">
            <span
              class="inline-block rounded-full border px-3.5 py-1 text-xs font-semibold tracking-wider transition-all duration-300 hover:bg-white hover:text-[#c2410c] hover:border-white cursor-pointer"
              style="background-color: rgba(255, 255, 255, 0.15); color: #ffffff; border-color: rgba(255, 255, 255, 0.3);">
              ESG Certified
            </span>
            <span
              class="inline-block rounded-full border px-3.5 py-1 text-xs font-semibold tracking-wider transition-all duration-300 hover:bg-white hover:text-[#c2410c] hover:border-white cursor-pointer"
              style="background-color: rgba(255, 255, 255, 0.15); color: #ffffff; border-color: rgba(255, 255, 255, 0.3);">
              B2B Ready
            </span>
          </div>
        </div>

        <div>
          <h4 class="text-sm font-bold uppercase tracking-widest text-white">Plataforma</h4>
          <ul class="mt-4 space-y-3 pl-0 list-none">
            <li><a href="#"
                class="text-sm text-white tracking-wide hover:text-orange-200 transition-colors duration-200 text-decoration-none">Empresas
                Doadoras</a></li>
            <li><a href="#"
                class="text-sm text-white tracking-wide hover:text-orange-200 transition-colors duration-200 text-decoration-none">Motoristas
                Parceiros</a></li>
            <li><a href="#"
                class="text-sm text-white tracking-wide hover:text-orange-200 transition-colors duration-200 text-decoration-none">ONGs
                Beneficiadas</a></li>
            <li><a href="#"
                class="text-sm text-white tracking-wide hover:text-orange-200 transition-colors duration-200 text-decoration-none">Relatórios
                ESG</a></li>
            <li><a href="#"
                class="text-sm text-white tracking-wide hover:text-orange-200 transition-colors duration-200 text-decoration-none">API</a>
            </li>
          </ul>
        </div>

        <div>
          <h4 class="text-sm font-bold uppercase tracking-widest text-white">Contato</h4>
          <ul class="mt-4 space-y-3 pl-0 list-none">
            <!-- Cada linha agora é um link interativo (<a>) para disparar o hover em toda a extensão -->
            <li>
              <a href="mailto:contato@ongsave.com"
                class="group flex items-center gap-3 text-sm text-white tracking-wide text-decoration-none cursor-pointer w-fit">
                <span
                  class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/15 transition-colors duration-300 group-hover:bg-white">
                  <i
                    class="fa-solid fa-envelope text-white text-xs transition-colors duration-300 group-hover:text-[#c2410c]"></i>
                </span>
                <span class="transition-colors duration-200 group-hover:text-orange-200">contato@ongsave.com</span>
              </a>
            </li>
            <li>
              <a href="tel:+551140000000"
                class="group flex items-center gap-3 text-sm text-white tracking-wide text-decoration-none cursor-pointer w-fit">
                <span
                  class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/15 transition-colors duration-300 group-hover:bg-white">
                  <i
                    class="fa-solid fa-phone text-white text-xs transition-colors duration-300 group-hover:text-[#c2410c]"></i>
                </span>
                <span class="transition-colors duration-200 group-hover:text-orange-200">+55 11 4000-0000</span>
              </a>
            </li>
            <li>
              <a href="#"
                class="group flex items-center gap-3 text-sm text-white tracking-wide text-decoration-none cursor-pointer w-fit">
                <span
                  class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/15 transition-colors duration-300 group-hover:bg-white">
                  <i
                    class="fa-solid fa-location-dot text-white text-xs transition-colors duration-300 group-hover:text-[#c2410c]"></i>
                </span>
                <span class="transition-colors duration-200 group-hover:text-orange-200">São Paulo, Brasil</span>
              </a>
            </li>
          </ul>
        </div>
      </div>

      <div class="mt-12 flex flex-col items-center justify-between gap-4 border-t border-white/20 pt-8 sm:flex-row">
        <p class="text-xs text-white tracking-wider opacity-90">© 2026 OngSave. Todos os direitos reservados.</p>
        <div class="flex gap-6">
          <a href="#"
            class="text-xs text-white tracking-wider hover:text-orange-200 transition-colors duration-200 text-decoration-none">Privacidade</a>
          <a href="#"
            class="text-xs text-white tracking-wider hover:text-orange-200 transition-colors duration-200 text-decoration-none">Termos</a>
          <a href="#"
            class="text-xs text-white tracking-wider hover:text-orange-200 transition-colors duration-200 text-decoration-none">LGPD</a>
        </div>
      </div>
    </div>
  </footer>
  <!-- Script Principal -->
  <script src="${ctx}/assets/js/index.js"></script>
</body>

</html>