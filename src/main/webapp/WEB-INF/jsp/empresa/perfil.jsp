<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Subscrição e Perfil - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/empresa/empresa.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="perfil" data-titulo="Subscrição &amp; Perfil" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">
  <div class="max-w-5xl mx-auto w-full space-y-6">

    <div class="card p-6 md:p-8">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-2xl bg-orange-50 text-orange-600 grid place-items-center text-xl"><i class="fa-solid fa-file-invoice-dollar"></i></div>
          <div>
            <span class="text-[10px] font-bold uppercase tracking-wider text-orange-600 bg-orange-50 px-2 py-0.5 rounded-full">Plano atual</span>
            <h2 id="p-nome" class="text-lg font-extrabold text-slate-900 mt-1"></h2>
            <p id="p-desc" class="text-xs text-slate-500"></p>
          </div>
        </div>
        <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200"><i class="fa-solid fa-circle-check"></i> Subscrição em dia</span>
      </div>
      <div class="grid sm:grid-cols-3 gap-4 mt-6">
        <div class="rounded-2xl border border-slate-200 p-4"><p class="rotulo">Próxima cobrança</p><p id="p-cobr" class="font-bold text-slate-900 mt-1"></p></div>
        <div class="rounded-2xl border border-slate-200 p-4"><p class="rotulo">Método de pagamento</p><p class="font-bold text-slate-900 mt-1">Cartão corporativo (**** 4092)</p></div>
        <div class="rounded-2xl border border-slate-200 p-4"><p class="rotulo">Uso da franquia</p><p id="p-eco" class="font-bold text-slate-900 mt-1"></p></div>
      </div>
      <p id="p-ret" class="text-sm text-slate-500 mt-5"></p>
      <div class="flex flex-wrap items-center justify-between gap-3 mt-5 pt-5 border-t border-slate-100">
        <p class="text-sm text-slate-500">Precisa expandir o volume de recolhas ou mudar de plano?</p>
        <button id="btn-plano" class="btn-lar">Alterar plano B2B</button>
      </div>
    </div>

    <div class="card p-6 md:p-8">
      <h3 class="font-bold text-slate-900"><i class="fa-solid fa-receipt text-orange-600 mr-1"></i> Faturas</h3>
      <p class="text-xs text-slate-500">Histórico das últimas cobranças.</p>
      <ul id="faturas" class="divide-y divide-slate-100 mt-3"></ul>
    </div>

    <form id="form-perfil" class="card p-6 md:p-8 space-y-6" novalidate>
      <div class="flex items-center gap-4 pb-6 border-b border-slate-100">
        <div class="w-12 h-12 rounded-2xl bg-orange-50 text-orange-600 grid place-items-center text-xl"><i class="fa-solid fa-building"></i></div>
        <div>
          <h3 class="text-lg font-extrabold text-slate-900">Dados da Empresa e Morada Operacional</h3>
          <p class="text-xs text-slate-500">A morada exata é a referência fixa para o cálculo do raio de 15 km dos motoristas.</p>
        </div>
      </div>

      <div class="grid sm:grid-cols-2 gap-5">
        <div><label class="rotulo" for="nome">Razão social / nome fantasia</label><input id="nome" class="campo" autocomplete="organization"><p class="msg-erro" data-e="nome"></p></div>
        <div><label class="rotulo" for="cnpj">CNPJ</label><input id="cnpj" class="campo" readonly></div>
        <div><label class="rotulo" for="email">E-mail corporativo de contacto</label><input id="email" type="email" class="campo" autocomplete="email"><p class="msg-erro" data-e="email"></p></div>
        <div><label class="rotulo" for="tel">Telefone / telemóvel</label><input id="tel" type="tel" class="campo" autocomplete="tel"><p class="msg-erro" data-e="tel"></p></div>
        <div><label class="rotulo" for="descarte">Custo mensal atual de descarte (R$)</label><input id="descarte" type="number" min="0" step="1" class="campo" placeholder="Opcional"><p class="text-xs text-slate-500 mt-1">Usado só para calcular a sua economia real no painel.</p><p class="msg-erro" data-e="descarte"></p></div>
      </div>

      <div class="grid sm:grid-cols-6 gap-5 pt-2">
        <div class="sm:col-span-2"><label class="rotulo" for="cep">CEP</label><input id="cep" class="campo" inputmode="numeric" maxlength="9" placeholder="00000-000"><p id="cep-st" class="text-xs text-slate-500 mt-1"></p><p class="msg-erro" data-e="cep"></p></div>
        <div class="sm:col-span-4"><label class="rotulo" for="rua">Logradouro (rua / avenida)</label><input id="rua" class="campo"><p class="msg-erro" data-e="rua"></p></div>
        <div class="sm:col-span-2"><label class="rotulo" for="num">Número</label><input id="num" class="campo"><p class="msg-erro" data-e="num"></p></div>
        <div class="sm:col-span-4"><label class="rotulo" for="compl">Complemento</label><input id="compl" class="campo"></div>
        <div class="sm:col-span-2"><label class="rotulo" for="bairro">Bairro</label><input id="bairro" class="campo"><p class="msg-erro" data-e="bairro"></p></div>
        <div class="sm:col-span-3"><label class="rotulo" for="cidade">Cidade</label><input id="cidade" class="campo"><p class="msg-erro" data-e="cidade"></p></div>
        <div class="sm:col-span-1"><label class="rotulo" for="uf">UF</label><input id="uf" class="campo uppercase" maxlength="2"><p class="msg-erro" data-e="uf"></p></div>
      </div>

      <p class="text-xs text-amber-600 font-semibold"><i class="fa-solid fa-triangle-exclamation"></i> Alterar estes campos recalcula o raio de proximidade dos motoristas parceiros.</p>

      <div class="flex flex-wrap justify-end items-center gap-3 pt-4 border-t border-slate-100">
        <span id="sujo-aviso" class="hidden mr-auto text-xs font-semibold text-amber-600"><i class="fa-solid fa-circle text-[7px] mr-1"></i> Alterações não guardadas</span>
        <button type="button" id="btn-descartar" class="btn-sec" disabled>Descartar</button>
        <button type="submit" id="btn-salvar" class="btn-lar" disabled>Guardar alterações <i class="fa-solid fa-check ml-1"></i></button>
      </div>
    </form>
  </div>
</section>

<div id="modal" class="hidden fixed inset-0 z-50 grid place-items-center p-4 bg-slate-900/50" role="dialog" aria-modal="true" aria-labelledby="m-tit">
  <div class="bg-white rounded-3xl max-w-3xl w-full p-6 md:p-8 space-y-5 max-h-[90vh] overflow-y-auto">
    <div class="flex items-start justify-between gap-3">
      <div><h3 id="m-tit" class="text-lg font-extrabold text-slate-900">Escolha o plano B2B</h3><p class="text-xs text-slate-500">Selecione o plano que combina com o volume de excedentes da empresa.</p></div>
      <button id="m-x" class="w-10 h-10 rounded-xl bg-slate-100 text-slate-600 hover:bg-orange-50 hover:text-orange-600" aria-label="Fechar"><i class="fa-solid fa-xmark"></i></button>
    </div>
    <div id="planos" class="grid md:grid-cols-3 gap-4"></div>
    <div class="flex justify-end gap-3"><button id="m-cancel" class="btn-sec">Cancelar</button><button id="m-ok" class="btn-lar">Confirmar plano</button></div>
  </div>
</div>
<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/empresa/empresa.js"></script>
<script src="${ctx}/assets/js/empresa/perfil.js"></script>
</body>

</html>
