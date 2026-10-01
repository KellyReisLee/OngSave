<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Carteira · OngSave</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/motorista/dashboard.css">
</head>
<body class="text-slate-800" data-pagina="carteira" data-titulo="Carteira Digital" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">
  <div class="grid sm:grid-cols-3 gap-4">
    <div class="card stat"><p class="rotulo">Saldo disponível</p><p id="saldo" class="text-3xl font-extrabold text-emerald-600 mt-1"></p></div>
    <div class="card stat"><p class="rotulo">A liberar (em análise)</p><p class="text-2xl font-extrabold mt-1">R$ 45,00</p><p class="text-xs text-slate-500 mt-1">Lote #8895, aguardando validação</p></div>
    <div class="card stat"><p class="rotulo">Recebido no mês</p><p class="text-2xl font-extrabold mt-1">R$ 1.840,00</p></div>
  </div>

  <div class="grid lg:grid-cols-3 gap-4">
    <div class="card stat lg:col-span-2">
      <h3 class="font-bold">Extrato</h3>
      <p class="text-xs text-slate-500">Fretes validados são pagos por um provedor de pagamento parceiro. Fretes com divergência de peso ou ocorrência informada pela ONG ficam em análise até a decisão da administração. Dados de exemplo.</p>
      <ul id="extrato" class="divide-y divide-slate-100 mt-2"></ul>
    </div>

    <div class="card stat self-start">
      <h3 class="font-bold">Sacar saldo</h3>
      <form id="form-saque" class="space-y-3 mt-3">
        <label class="rotulo block">Valor (R$)
          <input id="valor" type="number" min="1" step="0.01" class="campo mt-1" required>
        </label>
        <label class="rotulo block">Chave Pix
          <input id="pix" class="campo mt-1" autocomplete="off" required>
        </label>
        <button class="btn-verde w-full py-3"><i class="fa-solid fa-money-bill-transfer"></i> Solicitar saque</button>
      </form>
      <p class="text-xs text-slate-500 mt-3">O pedido de saque fica registado e é pago pelo provedor de pagamento parceiro.</p>
    </div>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/motorista/shell.js"></script>
<script src="${ctx}/assets/js/motorista/carteira.js"></script>
</body>
</html>
