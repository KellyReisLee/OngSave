<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Perfil e Veículo · OngSave</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/motorista/dashboard.css">
</head>
<body class="text-slate-800" data-pagina="perfil" data-titulo="Perfil e Veículo" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina">
  <form id="form-perfil" class="space-y-6" novalidate>

    <div class="card stat">
      <h3 class="font-bold mb-3"><i class="fa-solid fa-user text-emerald-600"></i> Dados pessoais</h3>
      <div class="grid sm:grid-cols-2 gap-4">
        <label class="rotulo">Nome completo<input id="m-nome" class="campo mt-1" maxlength="160" autocomplete="name" required></label>
        <label class="rotulo">CPF<input id="m-cpf" class="campo mt-1 bg-slate-100" readonly></label>
        <label class="rotulo">E-mail<input id="m-email" type="email" class="campo mt-1" maxlength="160" autocomplete="email" required></label>
        <label class="rotulo">Telefone / WhatsApp<input id="m-tel" type="tel" class="campo mt-1" maxlength="15" autocomplete="tel" required></label>
      </div>
    </div>

    <div class="card stat">
      <h3 class="font-bold mb-3"><i class="fa-solid fa-truck text-emerald-600"></i> Veículo</h3>
      <div class="grid sm:grid-cols-3 gap-4">
        <label class="rotulo">Tipo de veículo
          <select id="veiculo" class="campo mt-1">
            <option>Carro Económico</option><option>Camionete</option><option>Van</option><option>Caminhão</option>
          </select>
        </label>
        <label class="rotulo">Placa
          <input id="m-placa" class="campo mt-1 uppercase" maxlength="8" title="Ex.: ABC1D23 ou ABC1234" required>
        </label>
        <label class="rotulo">Modelo e ano<input id="m-modelo" class="campo mt-1" maxlength="80"></label>
      </div>
      <p id="dica-veiculo" class="text-xs text-slate-500 mt-3"></p>
    </div>

    <div class="card stat">
      <h3 class="font-bold mb-3"><i class="fa-solid fa-id-card text-emerald-600"></i> Habilitação (CNH)</h3>
      <div class="grid sm:grid-cols-3 gap-4">
        <label class="rotulo">Número da CNH<input id="m-cnh" class="campo mt-1" inputmode="numeric" maxlength="11" required></label>
        <label class="rotulo">Categoria
          <select id="m-cat" class="campo mt-1"><option>B</option><option>C</option><option>D</option><option>E</option></select>
        </label>
        <label class="rotulo">Validade <span id="cnh-status" class="badge ml-1"></span>
          <input id="cnh-val" type="date" class="campo mt-1" required>
        </label>
      </div>
    </div>

    <div class="card stat">
      <h3 class="font-bold"><i class="fa-solid fa-location-dot text-emerald-600"></i> Morada base</h3>
      <p class="text-xs text-slate-500 mb-3">Usada para avisar você das entregas dentro do raio de <span class="raio-km">15</span> km.</p>
      <div class="grid sm:grid-cols-4 gap-4">
        <label class="rotulo">CEP<input id="m-cep" class="campo mt-1" maxlength="9" inputmode="numeric" required></label>
        <label class="rotulo sm:col-span-2">Logradouro<input id="m-rua" class="campo mt-1" maxlength="160" required></label>
        <label class="rotulo">Número<input id="m-num" class="campo mt-1" maxlength="20" required></label>
        <label class="rotulo sm:col-span-2">Bairro<input id="m-bairro" class="campo mt-1" maxlength="80" required></label>
        <label class="rotulo">Cidade<input id="m-cidade" class="campo mt-1" maxlength="80" required></label>
        <label class="rotulo">UF<input id="m-uf" class="campo mt-1 uppercase" maxlength="2" required></label>
      </div>
      <p id="cep-st" class="text-xs text-slate-500 mt-2"></p>
      <p class="text-xs text-amber-600 mt-3"><i class="fa-solid fa-triangle-exclamation"></i> Alterar a morada recalcula as entregas que você recebe.</p>
    </div>

    <div class="flex justify-end">
      <button id="btn-salvar" class="btn-verde px-6 py-3">Guardar alterações <i class="fa-solid fa-check"></i></button>
    </div>
  </form>

  <div class="card stat mt-6">
    <h3 class="font-bold mb-3"><i class="fa-solid fa-scale-balanced text-emerald-600"></i> Como a plataforma funciona para você</h3>
    <ul class="space-y-3 text-sm text-slate-600 list-disc pl-5">
      <li>Você escolhe quais entregas aceitar e pode recusar qualquer uma, sem penalidade.</li>
      <li>Não há exclusividade nem meta mínima de entregas.</li>
      <li>O frete é fixo por tipo de veículo e aparece antes de você aceitar: <b id="tabela-frete"></b>.</li>
      <li>Sua localização só é compartilhada durante uma entrega ativa, com o seu consentimento. <span id="consent-st" class="font-semibold"></span></li>
      <li>Suspensão ou bloqueio só acontecem com motivo registrado, e você pode contestar a decisão.</li>
      <li>Os pagamentos são feitos por um provedor de pagamento parceiro. Fretes com divergência ficam em análise até a decisão da administração.</li>
    </ul>
    <div class="flex flex-wrap gap-3 mt-4">
      <button id="btn-contestar" class="btn-sec !py-2 text-xs">Contestar uma decisão</button>
      <button id="btn-revogar" class="btn-sec !py-2 text-xs">Revogar consentimento de localização</button>
    </div>
  </div>
</section>

<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/motorista/shell.js"></script>
<script src="${ctx}/assets/js/motorista/perfil.js"></script>
</body>
</html>
