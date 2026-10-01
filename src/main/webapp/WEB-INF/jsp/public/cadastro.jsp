<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Criar Conta - OngSave | Logística Inteligente B2B</title>
  <!-- Google Fonts Inter -->
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap"
    rel="stylesheet">
  <!-- FontAwesome para ícones -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <!-- Tailwind CSS CDN -->
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="${ctx}/assets/css/global.css">
  <link rel="stylesheet" href="${ctx}/assets/css/cadastro.css">
</head>

<body class="min-h-screen flex flex-col justify-between relative overflow-x-hidden">

  <!-- Elementos de Background decorativos (Orbs) -->
  <div class="glow-orb top-[-10%] left-[-10%] h-[30rem] w-[30rem] bg-orange-300/20"></div>
  <div class="glow-orb bottom-[-10%] right-[-10%] h-[30rem] w-[30rem] bg-emerald-300/15"></div>

  <!-- Header Simples com Botão de Voltar -->
  <header class="w-full py-6 px-6 max-w-7xl mx-auto flex items-center justify-between relative z-10">
    <a href="${ctx}/" class="group flex items-center gap-2.5 text-decoration-none">
      <div
        class="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 shadow-lg shadow-orange-500/30 transition-transform duration-300 group-hover:scale-110">
        <i class="fa-solid fa-leaf text-white text-sm"></i>
      </div>
      <span class="text-xl font-bold tracking-tight text-slate-800">
        Ong<span style="color: #ea580c;">Save</span>
      </span>
    </a>
    <a href="${ctx}/"
      class="text-sm font-semibold text-slate-500 hover:text-orange-600 transition-colors flex items-center gap-2">
      <i class="fa-solid fa-arrow-left"></i> Voltar ao início
    </a>
  </header>

  <!-- Conteúdo Principal -->
  <main class="flex-grow flex items-center justify-center px-4 py-12 relative z-10">
    <div class="w-full max-w-4xl mx-auto">

      <!-- Título da Página -->
      <div class="text-center mb-10 animate-fade-in-up">
        <span
          class="inline-block rounded-full border border-orange-200 bg-orange-50 px-4 py-1.5 text-xs font-semibold text-orange-600 mb-3"
          style="background-color: #fff7ed; border-color: #fed7aa; color: #ea580c;">
          Junte-se à Rede B2B
        </span>
        <h1 class="text-3xl font-extrabold text-slate-800 sm:text-4xl">Crie sua conta na OngSave</h1>
        <p class="mt-2 text-slate-500 text-sm sm:text-base">Selecione seu perfil operacional para personalizar sua
          experiência de impacto.</p>
      </div>

      <!-- Passo 1: Seleção de Tipo de Perfil (Cards interativos - 4 Colunas) -->
      <div id="step-selector" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6 mb-8 animate-fade-in-up"
        style="animation-delay: 0.1s;">

        <!-- Opção 1: Empresa -->
        <div onclick="selectProfile('empresa')" id="card-empresa"
          class="profile-card cursor-pointer rounded-2xl border-2 border-slate-200 bg-white p-6 transition-all duration-300 hover:border-orange-500 hover:shadow-xl group relative">
          <div class="absolute top-4 right-4 text-slate-300 group-hover:text-orange-600 transition-colors check-icon">
            <i class="fa-regular fa-circle text-xl"></i>
          </div>
          <div
            class="flex h-12 w-12 items-center justify-center rounded-xl bg-orange-50 text-orange-600 mb-4 group-hover:bg-orange-500 group-hover:text-white transition-colors"
            style="background-color: #ffedd5; color: #ea580c;">
            <i class="fa-solid fa-building text-lg"></i>
          </div>
          <h3 class="text-lg font-bold text-slate-800">Empresa Doadora</h3>
          <p class="mt-2 text-xs text-slate-500 leading-relaxed">Supermercados e indústrias que doam excedentes.</p>
        </div>

        <!-- Opção 2: Motorista -->
        <div onclick="selectProfile('motorista')" id="card-motorista"
          class="profile-card cursor-pointer rounded-2xl border-2 border-slate-200 bg-white p-6 transition-all duration-300 hover:border-emerald-500 hover:shadow-xl group relative">
          <div class="absolute top-4 right-4 text-slate-300 group-hover:text-emerald-600 transition-colors check-icon">
            <i class="fa-regular fa-circle text-xl"></i>
          </div>
          <div
            class="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 mb-4 group-hover:bg-emerald-500 group-hover:text-white transition-colors"
            style="background-color: #d1fae5; color: #10b981;">
            <i class="fa-solid fa-truck text-lg"></i>
          </div>
          <h3 class="text-lg font-bold text-slate-800">Motorista Parceiro</h3>
          <p class="mt-2 text-xs text-slate-500 leading-relaxed">Profissionais de transporte para rotas de curto raio.
          </p>
        </div>

        <!-- Opção 3: ONG -->
        <div onclick="selectProfile('ong')" id="card-ong"
          class="profile-card cursor-pointer rounded-2xl border-2 border-slate-200 bg-white p-6 transition-all duration-300 hover:border-blue-500 hover:shadow-xl group relative">
          <div class="absolute top-4 right-4 text-slate-300 group-hover:text-blue-600 transition-colors check-icon">
            <i class="fa-regular fa-circle text-xl"></i>
          </div>
          <div
            class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50 text-blue-600 mb-4 group-hover:bg-blue-500 group-hover:text-white transition-colors"
            style="background-color: #dbeafe; color: #3b82f6;">
            <i class="fa-solid fa-hands-holding-child text-lg"></i>
          </div>
          <h3 class="text-lg font-bold text-slate-800">Instituição (ONG)</h3>
          <p class="mt-2 text-xs text-slate-500 leading-relaxed">Organizações sociais que recebem os mantimentos.</p>
        </div>

        <!-- Opção 4: Administrador -->
        <div onclick="selectProfile('admin')" id="card-admin"
          class="profile-card cursor-pointer rounded-2xl border-2 border-slate-200 bg-white p-6 transition-all duration-300 hover:border-purple-500 hover:shadow-xl group relative">
          <div class="absolute top-4 right-4 text-slate-300 group-hover:text-purple-600 transition-colors check-icon">
            <i class="fa-regular fa-circle text-xl"></i>
          </div>
          <div
            class="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-50 text-purple-600 mb-4 group-hover:bg-purple-500 group-hover:text-white transition-colors"
            style="background-color: #f3e8ff; color: #9333ea;">
            <i class="fa-solid fa-user-shield text-lg"></i>
          </div>
          <h3 class="text-lg font-bold text-slate-800">Administrador</h3>
          <p class="mt-2 text-xs text-slate-500 leading-relaxed">Gestores da plataforma para moderação e governança.</p>
        </div>

      </div>

      <!-- Passo 2: Formulário Dinâmico de Cadastro (Oculto inicialmente) -->
      <div id="form-container"
        class="hidden bg-white rounded-3xl border border-slate-200 shadow-xl p-8 sm:p-10 animate-fade-in-up">

        <div class="flex items-center justify-between pb-6 mb-6 border-b border-slate-100">
          <div>
            <span class="text-xs font-bold uppercase tracking-wider text-orange-600" id="form-subtitle">Passo 2 de
              2</span>
            <h2 id="form-title" class="text-xl font-extrabold text-slate-800">Preencha os dados</h2>
          </div>
          <button onclick="resetSelection()"
            class="text-xs font-semibold text-slate-400 hover:text-slate-600 transition-colors underline cursor-pointer">
            Mudar perfil
          </button>
        </div>

        <c:if test="${not empty erro}">
          <div class="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 text-sm text-red-700 font-medium" role="alert">
            <i class="fa-solid fa-circle-exclamation mr-1"></i> <c:out value="${erro}"/>
          </div>
        </c:if>
        <form action="${ctx}/cadastro" method="post" onsubmit="handleRegistration(event)" class="space-y-6">
          <input type="hidden" name="csrf" value="${sessionScope.csrfToken}">
          <input type="hidden" name="perfil" id="input-perfil" value="${fn:escapeXml(param.perfil)}">

          <!-- Seção 1: Credenciais e Identificação Básica -->
          <div>
            <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-2">
              <i class="fa-solid fa-id-card-clip text-orange-500"></i> Informações Principais
            </h3>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label id="label-name" class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Nome
                  Completo</label>
                <div class="relative">
                  <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                      class="fa-solid fa-user"></i></span>
                  <input type="text" id="input-name" name="nome" value="${fn:escapeXml(param.nome)}" required placeholder="Ex: Carlos Alberto"
                    class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                </div>
              </div>

              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">E-mail
                  Corporativo</label>
                <div class="relative">
                  <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                      class="fa-solid fa-envelope"></i></span>
                  <input type="email" name="email" value="${fn:escapeXml(param.email)}" autocomplete="email" required placeholder="admin@ongsave.com.br"
                    class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                </div>
              </div>

              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Senha de
                  Acesso</label>
                <div class="relative">
                  <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                      class="fa-solid fa-lock"></i></span>
                  <input type="password" name="senha" minlength="8" autocomplete="new-password" required placeholder="Mínimo de 8 caracteres"
                    class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                </div>
              </div>

              <div>
                <label id="dynamic-label"
                  class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">CNPJ / CPF</label>
                <div class="relative">
                  <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                      class="fa-solid fa-file-invoice"></i></span>
                  <input type="text" id="dynamic-input" name="documento" required placeholder="000.000.000-00"
                    class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                </div>
              </div>
            </div>
          </div>

          <!-- Seção 2: Especificidades do Perfil -->
          <div class="pt-2">
            <h3 class="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-2"
              id="section-extra-title">
              <i class="fa-solid fa-circle-info text-orange-500" id="section-extra-icon"></i> <span
                id="section-extra-text">Detalhes Operacionais</span>
            </h3>

            <!-- Campos para Empresa ou ONG -->
            <div id="standard-extra-fields" class="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label id="extra-label"
                  class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Setor de Atuação</label>
                <div class="relative">
                  <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                      class="fa-solid fa-briefcase" id="extra-icon-elem"></i></span>
                  <input type="text" id="extra-input" name="extra" placeholder="Ex: Supermercado / Atacado"
                    class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                </div>
              </div>

              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Telefone /
                  WhatsApp</label>
                <div class="relative">
                  <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                      class="fa-solid fa-phone"></i></span>
                  <input type="tel" name="telefone" placeholder="(11) 99999-9999"
                    class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                </div>
              </div>
            </div>

            <!-- Campos específicos para o Motorista -->
            <div id="driver-extra-fields" class="hidden space-y-5">
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-5">
                <div>
                  <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Nº da CNH</label>
                  <div class="relative">
                    <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                        class="fa-solid fa-id-card"></i></span>
                    <input type="text" id="input-cnh" name="cnh" placeholder="00000000000"
                      class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                  </div>
                </div>

                <div>
                  <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Categoria
                    CNH</label>
                  <div class="relative">
                    <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                        class="fa-solid fa-layer-group"></i></span>
                    <select id="input-categoria" name="categoriaCnh"
                      class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 cursor-pointer">
                      <option value="">Selecione...</option>
                      <option value="B">Categoria B</option>
                      <option value="C">Categoria C</option>
                      <option value="D">Categoria D</option>
                      <option value="E">Categoria E</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Placa do
                    Veículo</label>
                  <div class="relative">
                    <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                        class="fa-solid fa-car"></i></span>
                    <input type="text" id="input-placa" name="placa" placeholder="ABC-1D23"
                      class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                  </div>
                </div>
              </div>
            </div>

            <!-- Campos específicos para o Administrador -->
            <div id="admin-extra-fields" class="hidden grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Cargo /
                  Função</label>
                <div class="relative">
                  <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                      class="fa-solid fa-user-gear"></i></span>
                  <input type="text" id="input-admin-cargo" name="cargo" placeholder="Ex: Gestor de Operações"
                    class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
                </div>
              </div>

              <div>
                <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Nível de Permissão
                  (Role)</label>
                <div class="relative">
                  <span class="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400"><i
                      class="fa-solid fa-shield-halved"></i></span>
                  <select id="input-admin-nivel" name="nivel"
                    class="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-sm text-slate-800 bg-slate-50/50 transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 cursor-pointer">
                    <option value="">Selecione o Nível...</option>
                    <option value="master">Administrador Master</option>
                    <option value="moderator">Moderador / Compliance</option>
                    <option value="support">Suporte Operacional</option>
                  </select>
                </div>
              </div>
            </div>

          </div>

          <!-- Termos e Condições -->
          <div class="flex items-center gap-3 pt-2">
            <input type="checkbox" required id="terms" name="termos" value="1"
              class="h-4 w-4 rounded border-slate-300 text-orange-600 focus:ring-orange-500 cursor-pointer">
            <label for="terms" class="text-xs text-slate-500 select-none">
              Concordo com as diretrizes de governança interna e os <a href="#"
                class="text-orange-600 font-semibold hover:underline">Termos de Segurança da Plataforma</a>.
            </label>
          </div>

          <!-- Botão Submeter -->
          <div class="pt-4">
            <button type="submit"
              class="w-full py-4 rounded-xl text-white font-bold text-base shadow-xl transition-all duration-300 hover:scale-[1.01] border-0 cursor-pointer flex items-center justify-center gap-2"
              style="background: linear-gradient(135deg, #f97316, #ea580c); box-shadow: 0 10px 25px -5px rgba(249, 115, 22, 0.4);">
              Concluir Cadastro de Administrador <i class="fa-solid fa-arrow-right"></i>
            </button>
          </div>

        </form>
      </div>

    </div>
  </main>

  <!-- Footer Simples -->
  <footer class="py-6 text-center text-xs text-slate-400 relative z-10 border-t border-slate-200/60 mt-12">
    <p>&copy; 2026 OngSave. Logística Inteligente B2B. Todos os direitos reservados.</p>
  </footer>
  <!-- Script Principal -->
  <script src="${ctx}/assets/js/cadastro.js"></script>
</body>

</html>