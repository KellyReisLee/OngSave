<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>404 · OngSave</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&display=swap" rel="stylesheet">
  <style>body { font-family: 'Inter', sans-serif; }</style>
</head>
<body class="bg-slate-50 min-h-screen grid place-items-center p-6 text-slate-800">
  <main class="max-w-md w-full bg-white border border-slate-200 rounded-3xl shadow-xl p-8 text-center space-y-4">
    <div class="w-16 h-16 mx-auto rounded-2xl bg-orange-100 text-orange-600 grid place-items-center text-2xl"><i class="fa-solid fa-map-location-dot"></i></div>
    <p class="text-5xl font-extrabold text-slate-900">404</p>
    <h1 class="text-xl font-bold">Página não encontrada</h1>
    <p class="text-sm text-slate-500">O endereço que você abriu não existe ou foi movido.</p>
    <div class="flex flex-wrap justify-center gap-3 pt-2">
      <a href="${ctx}/" class="py-3 px-5 rounded-2xl bg-orange-600 hover:bg-orange-700 text-white font-bold text-sm">Página inicial</a>
      <c:choose>
        <c:when test="${not empty sessionScope.usuarioLogado}">
          <a href="${ctx}/${sessionScope.usuarioLogado.perfil.segmento}/dashboard" class="py-3 px-5 rounded-2xl bg-slate-100 hover:bg-slate-200 font-bold text-sm">Voltar ao meu painel</a>
        </c:when>
        <c:otherwise>
          <a href="${ctx}/login" class="py-3 px-5 rounded-2xl bg-slate-100 hover:bg-slate-200 font-bold text-sm">Entrar</a>
        </c:otherwise>
      </c:choose>
    </div>
  </main>
</body>
</html>
