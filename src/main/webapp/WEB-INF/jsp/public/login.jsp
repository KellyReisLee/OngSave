<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Entrar - OngSave Brasil</title>
  <!-- Tailwind CSS -->
  <script src="https://cdn.tailwindcss.com"></script>
  <!-- FontAwesome Icons -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <!-- Google Fonts (Inter) -->
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap"
    rel="stylesheet">
  <style>
    body {
      font-family: 'Inter', sans-serif;
    }
  </style>
</head>

<body class="bg-slate-900 min-h-screen flex items-stretch text-slate-800 overflow-x-hidden">

  <!-- COLUNA DA ESQUERDA: Formulário de Login (Com identidade visual em Laranja/Orange) -->
  <div class="w-full lg:w-1/2 flex flex-col justify-between p-8 sm:p-12 lg:p-16 bg-white z-10 shadow-2xl">

    <!-- Topo da Coluna (Logo exata do projeto) -->
    <div class="flex justify-between items-center w-full max-w-md mx-auto">
      <a href="${ctx}/" class="flex items-center gap-2.5 group">
        <div
          class="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center text-white shadow-lg shadow-orange-500/30 group-hover:scale-105 transition-transform">
          <i class="fa-solid fa-leaf text-sm"></i>
        </div>
        <span class="text-xl font-extrabold tracking-tight text-slate-800">
          Ong<span style="color: #ea580c;">Save</span>
        </span>
      </a>
      <a href="${ctx}/cadastro"
        class="text-xs sm:text-sm font-semibold text-slate-500 hover:text-orange-600 transition-colors">
        Novo por aqui? <span class="underline font-bold text-orange-600">Criar conta</span>
      </a>
    </div>

    <!-- Centro: Formulário Profissional -->
    <div class="w-full max-w-md mx-auto my-auto py-8">

      <div class="mb-8">
        <span
          class="inline-block py-1 px-3 rounded-full bg-orange-50 text-orange-600 text-xs font-bold tracking-wide uppercase mb-3 border border-orange-200/60"
          style="background-color: #fff7ed; border-color: #fed7aa; color: #ea580c;">
          <i class="fa-solid fa-shield-halved mr-1"></i> Acesso Restrito B2B
        </span>
        <h1 class="text-3xl font-extrabold text-slate-900 tracking-tight">Bem-vindo de volta</h1>
        <p class="text-sm text-slate-500 mt-2 leading-relaxed">Insira suas credenciais corporativas para gerenciar
          operações e rotas.</p>
      </div>

      <%-- Mensagens vindas do FrontController (request) ou da URL (param) --%>
      <c:if test="${not empty erro}">
        <div class="mb-5 p-4 rounded-2xl bg-red-50 border border-red-200 text-sm text-red-700 font-medium" role="alert">
          <i class="fa-solid fa-circle-exclamation mr-1"></i> <c:out value="${erro}"/>
        </div>
      </c:if>
      <c:if test="${param.cadastro == 'ok'}">
        <div class="mb-5 p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-sm text-emerald-700 font-medium" role="status">
          <i class="fa-solid fa-circle-check mr-1"></i> Cadastro recebido! A equipa OngSave vai conferir os seus documentos. Assim que a conta for aprovada, poderá entrar com o e-mail e a senha que definiu.
        </div>
      </c:if>
      <c:if test="${param.inativa == '1'}">
        <div class="mb-5 p-4 rounded-2xl bg-amber-50 border border-amber-200 text-sm text-amber-700 font-medium" role="alert">
          <i class="fa-solid fa-triangle-exclamation mr-1"></i> A sua conta deixou de estar ativa. Fale com o suporte da OngSave.
        </div>
      </c:if>
      <c:if test="${param.senha == 'ok'}">
        <div class="mb-5 p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-sm text-emerald-700 font-medium" role="status">
          <i class="fa-solid fa-circle-check mr-1"></i> Senha atualizada! Entre com a sua nova senha.
        </div>
      </c:if>
      <c:if test="${param.saiu == '1'}">
        <div class="mb-5 p-4 rounded-2xl bg-slate-100 border border-slate-200 text-sm text-slate-600 font-medium" role="status">
          Sessão encerrada com segurança.
        </div>
      </c:if>

      <form action="${ctx}/login" method="post" class="space-y-5">
        <input type="hidden" name="csrf" value="${sessionScope.csrfToken}">
        <c:if test="${not empty param.next}">
          <input type="hidden" name="next" value="${fn:escapeXml(param.next)}">
        </c:if>

        <!-- E-mail / Usuário -->
        <div>
          <label class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">E-mail Corporativo ou
            CNPJ</label>
          <div class="relative">
            <span class="absolute inset-y-0 left-0 flex items-center pl-4 text-slate-400">
              <i class="fa-solid fa-envelope"></i>
            </span>
            <input type="text" id="login-email" name="email" value="${fn:escapeXml(param.email)}" autocomplete="username" required placeholder="empresa@exemplo.com"
              class="w-full pl-11 pr-4 py-3.5 bg-slate-50/80 border border-slate-200/80 rounded-2xl text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 focus:bg-white transition-all">
          </div>
        </div>

        <!-- Senha -->
        <div>
          <div class="flex justify-between items-center mb-2">
            <label class="block text-xs font-bold uppercase tracking-wider text-slate-600">Palavra-passe</label>
            <a href="${ctx}/recuperar-senha" class="text-xs font-semibold text-orange-600 hover:underline">Esqueceu-se?</a>
          </div>
          <div class="relative">
            <span class="absolute inset-y-0 left-0 flex items-center pl-4 text-slate-400">
              <i class="fa-solid fa-key"></i>
            </span>
            <input type="password" id="login-password" name="senha" autocomplete="current-password" required placeholder="••••••••"
              class="w-full pl-11 pr-4 py-3.5 bg-slate-50/80 border border-slate-200/80 rounded-2xl text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 focus:bg-white transition-all">
          </div>
        </div>

        <!-- Lembrar-me -->
        <div class="flex items-center text-sm pt-1">
          <label class="flex items-center gap-2.5 cursor-pointer select-none">
            <input type="checkbox" name="lembrar" value="1"
              class="w-4 h-4 rounded border-slate-300 text-orange-600 focus:ring-orange-500 cursor-pointer">
            <span class="text-slate-600 text-xs font-medium">Lembrar-me neste dispositivo</span>
          </label>
        </div>

        <!-- Botão de Entrar Profissional (Cor Laranja Padrão do Projeto) -->
        <button type="submit"
          class="w-full py-4 px-4 text-white font-bold rounded-2xl shadow-xl transition-all duration-300 flex items-center justify-center gap-2 group cursor-pointer"
          style="background: linear-gradient(135deg, #f97316, #ea580c); box-shadow: 0 10px 25px -5px rgba(249, 115, 22, 0.4);">
          <span>Aceder à Plataforma</span>
          <i class="fa-solid fa-arrow-right group-hover:translate-x-1.5 transition-transform"></i>
        </button>

      </form>

    </div>

    <!-- Rodapé da Coluna -->
    <div class="w-full max-w-md mx-auto text-center text-xs text-slate-400 pt-4">
      <p>&copy; 2026 OngSave Brasil. Logística Inteligente B2B.</p>
    </div>

  </div>

  <!-- COLUNA DA DIREITA: Imagem com Gradiente Baseado na Marca -->
  <div class="hidden lg:flex lg:w-1/2 relative bg-slate-950 items-center justify-center overflow-hidden">
    <!-- Imagem de Fundo de Alta Qualidade -->
    <img src="https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&w=1200&q=80"
      alt="Logística Solidária e Alimentos"
      class="absolute inset-0 w-full h-full object-cover opacity-35 mix-blend-overlay scale-105 hover:scale-100 transition-transform duration-1000">

    <!-- Gradiente escuro harmonizado com o laranja do projeto -->
    <div class="absolute inset-0 bg-gradient-to-br from-slate-950 via-slate-900/90 to-orange-950/60"></div>

    <!-- Conteúdo Conceitual com Efeito Glassmorphism -->
    <div class="relative z-10 p-16 max-w-xl text-white">
      <div
        class="w-14 h-14 rounded-2xl bg-white/10 border border-white/15 flex items-center justify-center text-orange-400 text-2xl mb-8 backdrop-blur-xl shadow-2xl">
        <i class="fa-solid fa-hand-holding-heart"></i>
      </div>

      <h2 class="text-4xl font-extrabold tracking-tight mb-6 leading-tight">
        Transformando excedentes em <span
          class="text-transparent bg-clip-text bg-gradient-to-r from-orange-400 to-amber-300">esperança e
          impacto</span>.
      </h2>

      <p class="text-slate-300 text-base leading-relaxed mb-10 font-light">
        Nossa tecnologia conecta cadeias de suprimentos e frotas parceiras para resgatar mantimentos com eficiência
        máxima, promovendo sustentabilidade corporativa e segurança alimentar.
      </p>

      <!-- Grid de Métricas de Destaque -->
      <div class="grid grid-cols-2 gap-6 p-6 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-md">
        <div>
          <p class="text-3xl font-extrabold text-orange-400">+50 mil</p>
          <p class="text-xs text-slate-400 mt-1 font-medium">Refeições viabilizadas</p>
        </div>
        <div>
          <p class="text-3xl font-extrabold text-amber-300">Zero Desperdício</p>
          <p class="text-xs text-slate-400 mt-1 font-medium">Missão ESG ativa</p>
        </div>
      </div>
    </div>
  </div>


</body>

</html>