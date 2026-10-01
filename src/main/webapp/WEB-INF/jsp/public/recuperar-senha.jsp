<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<%--
  Recuperar senha (sem e-mail nem código).
  GET  /recuperar-senha        -> formulário
  POST /recuperar-senha        -> FrontController.recuperarSenha()
       erro  -> volta aqui com ${erro} (caixa vermelha)
       ok    -> redireciona para /recuperar-senha?ok=1 (caixa verde + contagem até o login)
--%>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Recuperar senha - OngSave Brasil</title>
  <link rel="icon" href="${ctx}/assets/images/favicon.jpeg">
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    body { font-family: 'Inter', sans-serif; }
    .caixa-aviso { animation: surgir .25s ease-out; }
    @keyframes surgir { from { opacity: 0; transform: translateY(12px) scale(.97); } to { opacity: 1; transform: none; } }
  </style>
</head>

<body class="bg-slate-100 min-h-screen flex items-center justify-center p-4 text-slate-800">

  <main class="w-full max-w-md bg-white rounded-3xl shadow-2xl p-8 sm:p-10">

    <!-- Logo -->
    <a href="${ctx}/" class="flex items-center gap-2.5 mb-8">
      <div class="w-10 h-10 rounded-xl flex items-center justify-center text-white shadow-lg"
           style="background: linear-gradient(135deg, #f97316, #ea580c);">
        <i class="fa-solid fa-leaf text-sm"></i>
      </div>
      <span class="text-xl font-extrabold tracking-tight">Ong<span style="color:#ea580c;">Save</span></span>
    </a>

    <h1 class="text-2xl font-extrabold text-slate-900 tracking-tight">Criar uma nova senha</h1>
    <p class="text-sm text-slate-500 mt-2 mb-6">Confirme a sua conta e escolha a nova senha. Depois é só entrar.</p>

    <form id="form-senha" action="${ctx}/recuperar-senha" method="post" class="space-y-4" novalidate>
      <input type="hidden" name="csrf" value="${sessionScope.csrfToken}">

      <div>
        <label for="email" class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">E-mail da conta</label>
        <input type="email" id="email" name="email" required autocomplete="username"
               value="${fn:escapeXml(param.email)}" placeholder="voce@exemplo.com"
               class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
      </div>

      <div>
        <label for="verificacao" class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">CPF/CNPJ ou telefone cadastrado</label>
        <input type="text" id="verificacao" name="verificacao" required inputmode="numeric" autocomplete="off"
               value="${fn:escapeXml(param.verificacao)}" placeholder="Só para confirmar que a conta é sua"
               class="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
      </div>

      <div>
        <label for="senha" class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Nova senha</label>
        <div class="relative">
          <input type="password" id="senha" name="senha" required minlength="8" maxlength="128" autocomplete="new-password"
                 placeholder="Mínimo 8 caracteres"
                 class="w-full pl-4 pr-11 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
          <button type="button" data-alternar="senha" aria-label="Mostrar senha"
                  class="absolute inset-y-0 right-0 px-4 text-slate-400 hover:text-orange-600"><i class="fa-solid fa-eye"></i></button>
        </div>
      </div>

      <div>
        <label for="confirmacao" class="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">Confirmar nova senha</label>
        <div class="relative">
          <input type="password" id="confirmacao" name="confirmacao" required minlength="8" maxlength="128" autocomplete="new-password"
                 placeholder="Repita a senha"
                 class="w-full pl-4 pr-11 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500">
          <button type="button" data-alternar="confirmacao" aria-label="Mostrar senha"
                  class="absolute inset-y-0 right-0 px-4 text-slate-400 hover:text-orange-600"><i class="fa-solid fa-eye"></i></button>
        </div>
        <p id="aviso-local" class="hidden text-xs font-medium text-red-600 mt-2"></p>
      </div>

      <button type="submit" id="btn-enviar"
              class="w-full py-3.5 text-white font-bold rounded-2xl shadow-xl flex items-center justify-center gap-2 disabled:opacity-60"
              style="background: linear-gradient(135deg, #f97316, #ea580c);">
        <i class="fa-solid fa-key"></i> <span>Atualizar senha</span>
      </button>
    </form>

    <p class="text-center text-sm text-slate-500 mt-6">
      Lembrou-se? <a href="${ctx}/login" class="font-bold text-orange-600 hover:underline">Voltar ao login</a>
    </p>
  </main>

  <%-- ============ CAIXA DE SUCESSO ============ --%>
  <c:if test="${param.ok == '1'}">
    <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
      <div class="caixa-aviso w-full max-w-sm bg-white rounded-3xl shadow-2xl p-8 text-center" role="alertdialog" aria-labelledby="titulo-ok">
        <div class="w-16 h-16 mx-auto rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center text-3xl mb-4">
          <i class="fa-solid fa-circle-check"></i>
        </div>
        <h2 id="titulo-ok" class="text-xl font-extrabold text-slate-900">Senha alterada com sucesso!</h2>
        <p class="text-sm text-slate-500 mt-2">A mudança de senha foi confirmada. Já pode entrar com a nova senha.</p>
        <p class="text-xs text-slate-400 mt-4">A abrir a página de login em <b id="contagem">5</b> s…</p>
        <a href="${ctx}/login?senha=ok" class="mt-5 inline-flex w-full justify-center py-3 rounded-2xl text-white font-bold"
           style="background: linear-gradient(135deg, #10b981, #059669);">Ir para o login agora</a>
      </div>
    </div>
    <script>
      (function () {
        let s = 5;
        const el = document.getElementById('contagem');
        const t = setInterval(function () {
          s--; if (el) el.textContent = s;
          if (s <= 0) { clearInterval(t); window.location.replace('${ctx}/login?senha=ok'); }
        }, 1000);
      })();
    </script>
  </c:if>

  <%-- ============ CAIXA DE ERRO ============ --%>
  <c:if test="${not empty erro}">
    <div id="caixa-erro" class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
      <div class="caixa-aviso w-full max-w-sm bg-white rounded-3xl shadow-2xl p-8 text-center" role="alertdialog" aria-labelledby="titulo-erro">
        <div class="w-16 h-16 mx-auto rounded-full bg-red-100 text-red-600 flex items-center justify-center text-3xl mb-4">
          <i class="fa-solid fa-circle-xmark"></i>
        </div>
        <h2 id="titulo-erro" class="text-xl font-extrabold text-slate-900">Não foi possível alterar a senha</h2>
        <p class="text-sm text-red-700 mt-2"><c:out value="${erro}"/></p>
        <div class="mt-6 grid grid-cols-2 gap-3">
          <button type="button" onclick="document.getElementById('caixa-erro').remove()"
                  class="py-3 rounded-2xl border border-slate-200 font-bold text-slate-700 hover:bg-slate-50">Tentar de novo</button>
          <a href="${ctx}/login" class="py-3 rounded-2xl text-white font-bold"
             style="background: linear-gradient(135deg, #f97316, #ea580c);">Ir para o login</a>
        </div>
      </div>
    </div>
  </c:if>

  <script>
    // Mostrar/ocultar senha
    document.querySelectorAll('[data-alternar]').forEach(function (b) {
      b.addEventListener('click', function () {
        const campo = document.getElementById(b.dataset.alternar);
        const mostrar = campo.type === 'password';
        campo.type = mostrar ? 'text' : 'password';
        b.innerHTML = mostrar ? '<i class="fa-solid fa-eye-slash"></i>' : '<i class="fa-solid fa-eye"></i>';
      });
    });

    // Validação rápida no navegador (o servidor valida tudo de novo)
    document.getElementById('form-senha').addEventListener('submit', function (e) {
      const aviso = document.getElementById('aviso-local');
      const email = document.getElementById('email').value.trim();
      const verif = document.getElementById('verificacao').value.replace(/\D/g, '');
      const s1 = document.getElementById('senha').value;
      const s2 = document.getElementById('confirmacao').value;
      let msg = '';
      if (!email || !verif) msg = 'Preencha o e-mail e o CPF/CNPJ (ou telefone).';
      else if (s1.length < 8) msg = 'A nova senha precisa de pelo menos 8 caracteres.';
      else if (s1 !== s2) msg = 'As senhas não coincidem.';
      if (msg) {
        e.preventDefault();
        aviso.textContent = msg;
        aviso.classList.remove('hidden');
        return;
      }
      const btn = document.getElementById('btn-enviar');
      btn.disabled = true;
      btn.querySelector('span').textContent = 'A atualizar…';
    });
  </script>
</body>

</html>
