<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">

<head>
  <meta charset="UTF-8">
  <meta name="csrf" content="${sessionScope.csrfToken}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Registar Novo Lote - OngSave Brasil</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${ctx}/assets/css/empresa/empresa.css">
</head>

<body class="bg-slate-50 min-h-screen flex text-slate-800" data-pagina="nova-doacao" data-titulo="Registar Novo Lote" data-base="${ctx}" data-usuario="${fn:escapeXml(sessionScope.usuarioLogado.nome)}">

<section id="pagina" class="space-y-6">
  <form id="form-lote" class="grid lg:grid-cols-3 gap-6 items-start max-w-6xl mx-auto w-full" novalidate>
    <div class="lg:col-span-2 space-y-6">

      <div class="card p-6 md:p-8">
        <div class="flex items-center gap-4 mb-6 pb-6 border-b border-slate-100">
          <div class="w-12 h-12 rounded-2xl bg-orange-50 text-orange-600 grid place-items-center text-xl"><i class="fa-solid fa-boxes-packing"></i></div>
          <div>
            <h2 class="text-lg font-extrabold text-slate-900">Detalhes do Excedente Alimentar</h2>
            <p class="text-xs text-slate-500">Dados logísticos exatos para sincronizar com o marketplace de motoristas.</p>
          </div>
        </div>
        <div class="grid sm:grid-cols-2 gap-5">
          <div>
            <label class="rotulo" for="tipo">Tipo de excedente / categoria</label>
            <select id="tipo" class="campo">
              <option value="">Selecione a categoria...</option>
              <option>Laticínios e Frios</option>
              <option>Hortifrúti / Frutas e Verduras</option>
              <option>Padaria e Panificados</option>
              <option>Mercearia / Não Perecíveis</option>
              <option>Pratos Prontos / Marmitas</option>
            </select>
            <p class="msg-erro" data-e="tipo"></p>
          </div>
          <div>
            <label class="rotulo" for="kg">Peso total estimado (kg)</label>
            <input id="kg" type="number" min="1" step="0.1" inputmode="decimal" class="campo" placeholder="Ex: 120">
            <p class="msg-erro" data-e="kg"></p>
          </div>
          <div>
            <label class="rotulo" for="vol">Quantidade de volumes / caixas</label>
            <input id="vol" type="number" min="1" inputmode="numeric" class="campo" placeholder="Ex: 6">
            <p class="msg-erro" data-e="vol"></p>
          </div>
          <div>
            <label class="rotulo" for="cons">Conservação / temperatura</label>
            <select id="cons" class="campo">
              <option value="">Selecione o requisito térmico...</option>
              <option value="amb">Temperatura ambiente</option>
              <option value="ref">Refrigerado (2°C a 8°C)</option>
              <option value="cong">Congelado (abaixo de -18°C)</option>
            </select>
            <p class="msg-erro" data-e="cons"></p>
          </div>
          <div>
            <label class="rotulo" for="validade">Data e hora limite de validade</label>
            <input id="validade" type="datetime-local" class="campo">
            <p class="msg-erro" data-e="validade"></p>
          </div>
          <div>
            <span class="rotulo">Janela horária permitida para carga</span>
            <div class="flex items-center gap-2">
              <input id="j1" type="time" class="campo" aria-label="Início da janela">
              <span class="text-slate-400 mt-1 text-sm">às</span>
              <input id="j2" type="time" class="campo" aria-label="Fim da janela">
            </div>
            <p class="msg-erro" data-e="janela"></p>
          </div>
        </div>
      </div>

      <div class="card p-6 md:p-8 space-y-5">
        <h3 class="font-bold text-slate-900"><i class="fa-solid fa-truck-fast text-orange-600 mr-1"></i> Logística e destino</h3>
        <div>
          <label class="rotulo" for="veic">Veículo necessário</label>
          <select id="veic" class="campo">
            <option value="carro">Carro Económico / Utilitário (até 80 kg)</option>
            <option value="van">Van / Furgão (até 300 kg)</option>
            <option value="cam">Caminhão de Carga (acima de 300 kg)</option>
          </select>
          <p id="dica-veic" class="text-xs mt-1.5"></p>
        </div>
        <div>
          <label class="rotulo" for="ong">ONG de destino</label>
          <select id="ong" class="campo"></select>
          <p class="text-xs text-slate-500 mt-1.5">A opção automática usa a ONG mais próxima da sua loja.</p>
        </div>
        <div>
          <label class="rotulo" for="morada">Morada exata de recolha (ponto de partida)</label>
          <input id="morada" class="campo" readonly>
          <p class="text-xs text-slate-500 mt-1.5">Vem do seu perfil e define o raio de 15 km. <a href="${ctx}/empresa/perfil" class="text-orange-600 font-semibold hover:underline">Alterar morada</a></p>
        </div>
        <p id="motoristas" class="text-sm rounded-2xl bg-emerald-50 text-emerald-700 px-4 py-3"></p>
      </div>

      <div class="card p-6 md:p-8 space-y-5">
        <div>
          <span class="rotulo">Fotografia do lote lacrado na origem (anti-fraude)</span>
          <label id="drop" for="foto" class="mt-1.5 flex flex-col items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-slate-300 bg-slate-50 hover:border-orange-400 hover:bg-orange-50/40 cursor-pointer p-8 text-center transition-colors">
            <span id="drop-vazio" class="flex flex-col items-center gap-2">
              <span class="w-12 h-12 rounded-full bg-orange-100 text-orange-600 grid place-items-center"><i class="fa-solid fa-camera"></i></span>
              <span class="text-sm font-bold text-slate-700">Clique, arraste ou tire uma foto do lote</span>
              <span class="text-xs text-slate-400">PNG ou JPG até 5 MB. Obrigatória para a validação inicial.</span>
            </span>
            <img id="prev" class="hidden max-h-48 rounded-xl" alt="Prévia da foto do lote">
            <input id="foto" type="file" accept="image/png,image/jpeg" class="sr-only">
          </label>
          <p class="msg-erro" data-e="foto"></p>
        </div>
        <div>
          <label class="rotulo" for="obs">Instruções especiais para o motorista</label>
          <textarea id="obs" rows="3" maxlength="300" class="campo" placeholder="Ex: retirada pelos fundos do armazém, carregar com paleteira..."></textarea>
          <p class="text-right text-xs text-slate-400"><span id="obs-n">0</span>/300</p>
        </div>
        <div>
          <label class="flex gap-3 text-sm text-slate-600 cursor-pointer">
            <input id="termo" type="checkbox" class="mt-1 accent-orange-600 w-4 h-4 shrink-0">
            <span>Declaro que os alimentos estão dentro da validade e próprios para consumo humano, com propriedades nutricionais e segurança mantidas e cadeia de frio respeitada quando exigida (Lei 15.224/2025 e normas sanitárias). Estou de acordo com o termo de responsabilidade técnica do plano B2B.</span>
          </label>
          <p class="msg-erro" data-e="termo"></p>
        </div>
      </div>
    </div>

    <aside class="card p-6 space-y-5 lg:sticky lg:top-28" aria-label="Resumo do lote">
      <h3 class="font-bold text-slate-900">Resumo do lote</h3>
      <dl class="space-y-3 text-sm">
        <div class="flex justify-between gap-3"><dt class="text-slate-500">Categoria</dt><dd id="r-tipo" class="font-semibold text-right">-</dd></div>
        <div class="flex justify-between gap-3"><dt class="text-slate-500">Peso</dt><dd id="r-kg" class="font-semibold text-right">-</dd></div>
        <div class="flex justify-between gap-3"><dt class="text-slate-500">Veículo</dt><dd id="r-veic" class="font-semibold text-right">-</dd></div>
        <div class="flex justify-between gap-3"><dt class="text-slate-500">Prazo</dt><dd id="r-prazo" class="font-semibold text-right">-</dd></div>
        <div class="flex justify-between gap-3"><dt class="text-slate-500">Destino</dt><dd id="r-ong" class="font-semibold text-right">-</dd></div>
      </dl>
      <p id="r-plano" class="text-xs rounded-2xl bg-slate-50 px-4 py-3 text-slate-600"></p>
      <div class="grid grid-cols-2 gap-3 text-center">
        <div class="rounded-2xl bg-orange-50 p-3"><p id="r-ref" class="text-lg font-extrabold text-orange-600">0</p><p class="text-[11px] font-semibold text-slate-500">refeições</p></div>
        <div class="rounded-2xl bg-emerald-50 p-3"><p id="r-co2" class="text-lg font-extrabold text-emerald-600">0 t</p><p class="text-[11px] font-semibold text-slate-500">CO₂ evitado</p></div>
      </div>
      <button id="btn-pub" type="submit" class="btn-lar w-full"><i class="fa-solid fa-paper-plane mr-1"></i> Publicar lote</button>
      <a href="${ctx}/empresa/dashboard" class="btn-sec block text-center">Cancelar</a>
      <p class="text-[11px] text-center text-slate-400">Rascunho salvo automaticamente neste navegador.</p>
    </aside>
  </form>
</section>


<%@ include file="/WEB-INF/jspf/dados.jspf" %>
<script src="${ctx}/assets/js/plataforma.js"></script>
<script src="${ctx}/assets/js/empresa/empresa.js"></script>
<script src="${ctx}/assets/js/empresa/nova-doacao.js"></script>
</body>

</html>
