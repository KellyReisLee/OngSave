<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Perfil da Instituição - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/ong/ong.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="perfil" data-titulo="Perfil da Instituição" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">
  <div class="max-w-5xl mx-auto w-full space-y-6">

    <div class="card p-6 md:p-8">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 grid place-items-center text-xl"><i class="fa-solid fa-shield-check"></i></div>
          <div>
            <h2 class="text-lg font-extrabold text-slate-900">Cadastro aprovado</h2>
            <p class="text-xs text-slate-500">Validado pela administração da plataforma. Mantenha os documentos em dia para continuar recebendo doações.</p>
          </div>
        </div>
        <span id="alv-badge" class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border"></span>
      </div>
      <h3 class="rotulo mt-6 mb-1">Documentos da instituição</h3>
      <ul id="docs" class="divide-y divide-slate-100"></ul>
      <p class="text-xs text-slate-400 mt-3">PDF, PNG ou JPG até 5 MB. Documentos novos passam por análise antes de valer.</p>
    </div>

    <form id="form-perfil" class="card p-6 md:p-8 space-y-8" novalidate>

      <div class="space-y-5">
        <div class="flex items-center gap-4 pb-5 border-b border-slate-100">
          <div class="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 grid place-items-center text-xl"><i class="fa-solid fa-building-ngo"></i></div>
          <div><h3 class="text-lg font-extrabold text-slate-900">Dados da Instituição</h3><p class="text-xs text-slate-500">Como a instituição aparece para empresas e motoristas.</p></div>
        </div>
        <div class="grid sm:grid-cols-2 gap-5">
          <div><label class="rotulo" for="nome">Razão social</label><input id="nome" class="campo" autocomplete="organization"><p class="msg-erro" data-e="nome"></p></div>
          <div><label class="rotulo" for="fantasia">Nome da instituição (exibição)</label><input id="fantasia" class="campo"><p class="msg-erro" data-e="fantasia"></p></div>
          <div><label class="rotulo" for="cnpj">CNPJ da ONG</label><input id="cnpj" class="campo" readonly></div>
          <div><label class="rotulo" for="resp">Responsável legal</label><input id="resp" class="campo" autocomplete="name"><p class="msg-erro" data-e="resp"></p></div>
          <div><label class="rotulo" for="email">E-mail de contato</label><input id="email" type="email" class="campo" autocomplete="email"><p class="msg-erro" data-e="email"></p></div>
          <div><label class="rotulo" for="tel">Telefone / WhatsApp</label><input id="tel" type="tel" class="campo" autocomplete="tel"><p class="msg-erro" data-e="tel"></p></div>
          <div><label class="rotulo" for="familias">Famílias atendidas</label><input id="familias" type="number" min="1" class="campo"><p class="msg-erro" data-e="familias"></p></div>
        </div>
      </div>

      <div class="space-y-5">
        <div class="pb-3 border-b border-slate-100">
          <h3 class="font-bold text-slate-900"><i class="fa-solid fa-location-dot text-blue-600 mr-1"></i> Morada de recebimento</h3>
          <p class="text-xs text-slate-500">É o endereço que os motoristas usam para entregar. Ele também define quais doações aparecem para vocês.</p>
        </div>
        <div class="grid sm:grid-cols-6 gap-5">
          <div class="sm:col-span-2"><label class="rotulo" for="cep">CEP</label><input id="cep" class="campo" inputmode="numeric" maxlength="9" placeholder="00000-000"><p id="cep-st" class="text-xs text-slate-500 mt-1"></p><p class="msg-erro" data-e="cep"></p></div>
          <div class="sm:col-span-4"><label class="rotulo" for="rua">Logradouro</label><input id="rua" class="campo"><p class="msg-erro" data-e="rua"></p></div>
          <div class="sm:col-span-2"><label class="rotulo" for="num">Número</label><input id="num" class="campo"><p class="msg-erro" data-e="num"></p></div>
          <div class="sm:col-span-4"><label class="rotulo" for="compl">Complemento</label><input id="compl" class="campo"></div>
          <div class="sm:col-span-2"><label class="rotulo" for="bairro">Bairro</label><input id="bairro" class="campo"><p class="msg-erro" data-e="bairro"></p></div>
          <div class="sm:col-span-3"><label class="rotulo" for="cidade">Cidade</label><input id="cidade" class="campo"><p class="msg-erro" data-e="cidade"></p></div>
          <div class="sm:col-span-1"><label class="rotulo" for="uf">UF</label><input id="uf" class="campo uppercase" maxlength="2"><p class="msg-erro" data-e="uf"></p></div>
        </div>
      </div>

      <div class="space-y-5">
        <div class="pb-3 border-b border-slate-100">
          <h3 class="font-bold text-slate-900"><i class="fa-solid fa-warehouse text-blue-600 mr-1"></i> Capacidade e operação</h3>
          <p class="text-xs text-slate-500">Ajuda empresas e motoristas a enviarem só o que vocês conseguem receber.</p>
        </div>
        <div class="grid sm:grid-cols-4 gap-5">
          <div><label class="rotulo" for="capKg">Capacidade diária (kg)</label><input id="capKg" type="number" min="1" class="campo"><p class="msg-erro" data-e="capKg"></p></div>
          <div><label class="rotulo" for="horaIni">Recebe a partir de</label><input id="horaIni" type="time" class="campo"></div>
          <div><label class="rotulo" for="horaFim">Recebe até</label><input id="horaFim" type="time" class="campo"><p class="msg-erro" data-e="horaFim"></p></div>
          <div><label class="rotulo" for="alvara">Validade do alvará</label><input id="alvara" type="date" class="campo"><p class="msg-erro" data-e="alvara"></p></div>
        </div>
        <label class="flex gap-3 text-sm text-slate-600 cursor-pointer"><input id="camara" type="checkbox" class="mt-1 accent-blue-600 w-4 h-4 shrink-0"><span>A instituição tem câmara fria e pode receber alimentos refrigerados ou congelados.</span></label>
        <div>
          <span class="rotulo">Categorias que a instituição aceita receber</span>
          <div id="cats" class="grid sm:grid-cols-2 gap-2 mt-2"></div>
          <p class="msg-erro" data-e="cats"></p>
        </div>
      </div>

      <div class="flex flex-wrap justify-end items-center gap-3 pt-5 border-t border-slate-100">
        <span id="sujo-aviso" class="hidden mr-auto text-xs font-semibold text-amber-600"><i class="fa-solid fa-circle text-[7px] mr-1"></i> Alterações não guardadas</span>
        <button type="button" id="btn-descartar" class="btn-sec" disabled>Descartar</button>
        <button type="submit" id="btn-salvar" class="btn-lar" disabled>Guardar alterações <i class="fa-solid fa-check ml-1"></i></button>
      </div>
    </form>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/ong/ong.js"></script>
<script src="${ctx}/assets/js/ong/perfil.js"></script>
</body>

</html>
