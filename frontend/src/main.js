import "./style.css";

const app = document.querySelector("#app");
const API = "";

function getToken() {
  return localStorage.getItem("bikes_token");
}

function getRole() {
  const token = getToken();
  if (!token) return null;
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = JSON.parse(decodeURIComponent(atob(payload).split("").map(
      (char) => "%" + ("00" + char.charCodeAt(0).toString(16)).slice(-2)
    ).join("")));
    return json.role || null;
  } catch {
    return null;
  }
}

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`${API}${path}`, { ...options, headers });
  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }
  if (!response.ok) {
    throw new Error(body?.message || body?.errors?.[0]?.message || body || `Erro ${response.status}`);
  }
  return body;
}

function escapeHtml(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("pt-BR");
}

function jsonTable(items) {
  if (!Array.isArray(items) || !items.length) return "<p class=\"empty\">Nenhum registro encontrado.</p>";
  const keys = Object.keys(items[0]).filter((key) =>
    !["password", "codigoSeguranca", "numero"].includes(key));
  return `<div class="table-wrap"><table><thead><tr>${keys.map((key) =>
    `<th>${escapeHtml(key)}</th>`).join("")}</tr></thead><tbody>${items.map((item) =>
      `<tr>${keys.map((key) => `<td>${escapeHtml(
        typeof item[key] === "object" ? JSON.stringify(item[key]) :
        key.toLowerCase().includes("data") || key.toLowerCase().includes("hora")
          ? formatDate(item[key])
          : item[key]
      )}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`;
}

function resultBlock(id = "result") {
  return `<pre id="${id}" class="result">Selecione uma ação.</pre>`;
}

function shell(content) {
  app.innerHTML = `
    <header>
      <div>
        <h1>Bikes</h1>
        <p>Painel de testes dos recursos da API</p>
      </div>
      <div class="auth-summary">
        <span>${escapeHtml(getRole() || "Sem sessão")}</span>
        <button id="logout" class="danger">Sair</button>
      </div>
    </header>
    ${content}
  `;
  document.querySelector("#logout")?.addEventListener("click", () => {
    localStorage.removeItem("bikes_token");
    renderLogin();
  });
}

function renderLogin() {
  shell(`
    <main class="login-grid">
      <section class="card">
        <h2>Entrar</h2>
        <form id="login-form">
          <label>E-mail<input name="username" type="email" required value="maria@gmail.com"></label>
          <label>Senha (6 caracteres)<input name="password" type="password" minlength="6" maxlength="6" required value="123456"></label>
          <button type="submit">Entrar</button>
        </form>
        ${resultBlock("login-result")}
      </section>
      <section class="card">
        <h2>Cadastrar usuário</h2>
        <form id="register-form">
          <label>E-mail<input name="username" type="email" required></label>
          <label>Senha (6 caracteres)<input name="password" type="password" minlength="6" maxlength="6" required></label>
          <button type="submit">Cadastrar</button>
        </form>
        ${resultBlock("register-result")}
      </section>
    </main>
  `);

  document.querySelector("#login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    try {
      const result = await api("/api/v1/auth", {
        method: "POST",
        body: JSON.stringify(data)
      });
      localStorage.setItem("bikes_token", result.token);
      renderDashboard();
    } catch (error) {
      document.querySelector("#login-result").textContent = error.message;
    }
  });

  document.querySelector("#register-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    try {
      const result = await api("/api/v1/usuarios", {
        method: "POST",
        body: JSON.stringify(data)
      });
      document.querySelector("#register-result").textContent =
        `Usuário criado: ${JSON.stringify(result, null, 2)}`;
    } catch (error) {
      document.querySelector("#register-result").textContent = error.message;
    }
  });
}

function renderDashboard() {
  const role = getRole();
  const admin = role === "ROLE_ADMIN";
  shell(`
    <nav>
      ${["Clientes", "Bicicletas", "Locações", "Devoluções", ...(admin ? ["Relatórios"] : [])]
        .map((tab, index) => `<button class="tab ${index === 0 ? "active" : ""}" data-tab="${tab}">${tab}</button>`)
        .join("")}
    </nav>
    <main id="dashboard"></main>
  `);
  document.querySelectorAll(".tab").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll(".tab").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      renderTab(button.dataset.tab);
    });
  });
  renderTab("Clientes");
}

function renderTab(tab) {
  const area = document.querySelector("#dashboard");
  if (tab === "Clientes") renderClientes(area);
  if (tab === "Bicicletas") renderBicicletas(area);
  if (tab === "Locações") renderLocacoes(area);
  if (tab === "Devoluções") renderDevolucoes(area);
  if (tab === "Relatórios") renderRelatorios(area);
}

async function showRequest(url, options, targetId) {
  try {
    const result = await api(url, options);
    document.querySelector("#" + targetId).textContent =
      JSON.stringify(result, null, 2);
  } catch (error) {
    document.querySelector("#" + targetId).textContent = error.message;
  }
}

function renderClientes(area) {
  const admin = getRole() === "ROLE_ADMIN";
  area.innerHTML = `
    <section class="grid">
      <article class="card">
        <h2>Cadastrar cliente</h2>
        <form id="client-form">
          <label>Nome<input name="nome" required></label>
          <label>CPF (11 dígitos)<input name="cpf" minlength="11" maxlength="14" required></label>
          <button>Cadastrar</button>
        </form>
        ${resultBlock("client-result")}
      </article>
      <article class="card">
        <h2>Consultas</h2>
        <div class="button-row">
          <button id="client-me">Meus dados</button>
          ${admin ? '<button id="clients-all">Todos os clientes</button>' : ""}
        </div>
        ${resultBlock("client-list")}
      </article>
    </section>
  `;
  document.querySelector("#client-form").addEventListener("submit", (event) => {
    event.preventDefault();
    showRequest("/api/v1/clientes", {
      method: "POST",
      body: JSON.stringify(Object.fromEntries(new FormData(event.currentTarget)))
    }, "client-result");
  });
  document.querySelector("#client-me").addEventListener("click", async () => {
    try {
      document.querySelector("#client-list").textContent =
        JSON.stringify(await api("/api/v1/clientes/me"), null, 2);
    } catch (error) {
      document.querySelector("#client-list").textContent = error.message;
    }
  });
  document.querySelector("#clients-all")?.addEventListener("click", async () => {
    try {
      const items = await api("/api/v1/clientes");
      document.querySelector("#client-list").innerHTML = jsonTable(items);
    } catch (error) {
      document.querySelector("#client-list").textContent = error.message;
    }
  });
}

function renderBicicletas(area) {
  const admin = getRole() === "ROLE_ADMIN";
  area.innerHTML = admin ? `
    <section class="grid">
      <article class="card">
        <h2>Cadastrar bicicleta</h2>
        <form id="bike-form">
          <label>Código<input name="codigo" required></label>
          <label>Status<select name="status">
            <option>LIVRE</option><option>OCUPADA</option><option>EM_MANUTENCAO</option>
          </select></label>
          <button>Cadastrar</button>
        </form>
        ${resultBlock("bike-create")}
      </article>
      <article class="card">
        <h2>Atualizar / remover</h2>
        <form id="bike-update">
          <label>Código atual<input name="codigoAtual" required></label>
          <label>Novo código<input name="codigo" required></label>
          <label>Status<select name="status">
            <option>LIVRE</option><option>OCUPADA</option><option>EM_MANUTENCAO</option>
          </select></label>
          <div class="button-row"><button type="submit">Atualizar</button>
          <button type="button" id="bike-delete" class="danger">Remover</button></div>
        </form>
        ${resultBlock("bike-update-result")}
      </article>
    </section>
    <section class="card">
      <div class="button-row"><h2>Bicicletas</h2><button id="bike-list">Atualizar lista</button></div>
      <form id="bike-search" class="inline-form">
        <label>Código para localizar<input name="codigo" required></label>
        <button>Buscar pelo código</button>
      </form>
      <div id="bike-list-result"></div>
    </section>
  ` : `<section class="card"><h2>Bicicletas</h2><p>Somente administradores podem gerir bicicletas.</p></section>`;

  if (!admin) return;
  document.querySelector("#bike-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    showRequest("/api/v1/bicicletas", { method: "POST", body: JSON.stringify(data) }, "bike-create");
  });
  document.querySelector("#bike-update").addEventListener("submit", (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    showRequest(`/api/v1/bicicletas/${encodeURIComponent(data.codigoAtual)}`, {
      method: "PUT",
      body: JSON.stringify({ codigo: data.codigo, status: data.status })
    }, "bike-update-result");
  });
  document.querySelector("#bike-delete").addEventListener("click", () => {
    const code = document.querySelector('[name="codigoAtual"]').value;
    if (!code) return;
    api(`/api/v1/bicicletas/${encodeURIComponent(code)}`, { method: "DELETE" })
      .then(() => document.querySelector("#bike-update-result").textContent = "Bicicleta removida.")
      .catch((error) => document.querySelector("#bike-update-result").textContent = error.message);
  });
  document.querySelector("#bike-list").addEventListener("click", async () => {
    try {
      document.querySelector("#bike-list-result").innerHTML =
        jsonTable(await api("/api/v1/bicicletas"));
    } catch (error) {
      document.querySelector("#bike-list-result").textContent = error.message;
    }
  });
  document.querySelector("#bike-search").addEventListener("submit", async (event) => {
    event.preventDefault();
    const { codigo } = Object.fromEntries(new FormData(event.currentTarget));
    try {
      document.querySelector("#bike-list-result").innerHTML =
        jsonTable([await api(`/api/v1/bicicletas/${encodeURIComponent(codigo)}`)]);
    } catch (error) {
      document.querySelector("#bike-list-result").textContent = error.message;
    }
  });
  document.querySelector("#bike-list").click();
}

function renderLocacoes(area) {
  const admin = getRole() === "ROLE_ADMIN";
  area.innerHTML = `
    <section class="grid">
      <article class="card">
        <h2>Solicitar aluguel</h2>
        <form id="rent-form">
          <label>Devolução prevista<input name="dataHoraPrevistaDevolucao" type="datetime-local" required></label>
          <button>Solicitar bicicleta livre</button>
        </form>
        ${resultBlock("rent-result")}
      </article>
      <article class="card">
        <h2>Efetivar locação</h2>
        <form id="effect-form">
          <label>ID da locação<input name="id" type="number" required></label>
          <label>Titular<input name="titular" required></label>
          <label>Número do cartão<input name="numero" required minlength="13" maxlength="19"></label>
          <label>Validade MM/AA<input name="validade" required placeholder="12/30"></label>
          <label>CVV<input name="codigoSeguranca" required minlength="3" maxlength="4"></label>
          <button>Efetivar</button>
        </form>
        ${resultBlock("effect-result")}
      </article>
    </section>
    <section class="card">
      <div class="button-row"><h2>Locações</h2>
        <button id="locations-mine">Minhas locações</button>
        ${admin ? '<button id="locations-all">Todas</button>' : ""}
      </div>
      <div id="locations-list"></div>
    </section>
  `;
  document.querySelector("#rent-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    showRequest("/api/v1/locacoes/solicitacoes", {
      method: "POST",
      body: JSON.stringify({
        dataHoraPrevistaDevolucao: `${data.dataHoraPrevistaDevolucao}:00`
      })
    }, "rent-result");
  });
  document.querySelector("#effect-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    showRequest(`/api/v1/locacoes/${data.id}/efetivar`, {
      method: "POST",
      body: JSON.stringify({
        cartao: {
          titular: data.titular,
          numero: data.numero,
          validade: data.validade,
          codigoSeguranca: data.codigoSeguranca
        }
      })
    }, "effect-result");
  });
  document.querySelector("#locations-mine").addEventListener("click", async () => {
    try {
      document.querySelector("#locations-list").innerHTML =
        jsonTable(await api("/api/v1/locacoes/me"));
    } catch (error) {
      document.querySelector("#locations-list").textContent = error.message;
    }
  });
  document.querySelector("#locations-all")?.addEventListener("click", async () => {
    try {
      document.querySelector("#locations-list").innerHTML =
        jsonTable(await api("/api/v1/locacoes"));
    } catch (error) {
      document.querySelector("#locations-list").textContent = error.message;
    }
  });
  document.querySelector("#locations-mine").click();
}

function renderDevolucoes(area) {
  const admin = getRole() === "ROLE_ADMIN";
  area.innerHTML = `
    <section class="grid">
      <article class="card">
        <h2>Registrar devolução</h2>
        <form id="return-form">
          <label>ID da locação<input name="locacaoId" type="number" required></label>
          <label><input name="problema" type="checkbox"> Problema identificado</label>
          <label>Observação<textarea name="observacao" maxlength="500"></textarea></label>
          <button>Registrar devolução</button>
        </form>
        ${resultBlock("return-result")}
      </article>
      <article class="card">
        <h2>Alterar bicicleta da devolução</h2>
        ${admin ? `
          <form id="return-status-form">
            <label>ID da devolução<input name="devolucaoId" type="number" required></label>
            <label>Status<select name="status"><option>LIVRE</option><option>EM_MANUTENCAO</option></select></label>
            <button>Alterar</button>
          </form>
          ${resultBlock("return-status-result")}
        ` : "<p>Somente administradores podem alterar o status.</p>"}
      </article>
    </section>
    <section class="card">
      <div class="button-row"><h2>Devoluções</h2>
        <button id="returns-mine">Minhas devoluções</button>
        ${admin ? '<button id="returns-all">Todas</button>' : ""}
      </div>
      <div id="returns-list"></div>
    </section>
  `;
  document.querySelector("#return-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    showRequest(`/api/v1/devolucoes/locacao/${data.locacaoId}`, {
      method: "POST",
      body: JSON.stringify({
        problemaIdentificado: data.problema === "on",
        observacao: data.observacao || null
      })
    }, "return-result");
  });
  document.querySelector("#return-status-form")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    showRequest(`/api/v1/devolucoes/${data.devolucaoId}/bicicleta-status?status=${data.status}`, {
      method: "PATCH"
    }, "return-status-result");
  });
  document.querySelector("#returns-mine").addEventListener("click", async () => {
    try {
      document.querySelector("#returns-list").innerHTML =
        jsonTable(await api("/api/v1/devolucoes/me"));
    } catch (error) {
      document.querySelector("#returns-list").textContent = error.message;
    }
  });
  document.querySelector("#returns-all")?.addEventListener("click", async () => {
    try {
      document.querySelector("#returns-list").innerHTML =
        jsonTable(await api("/api/v1/devolucoes"));
    } catch (error) {
      document.querySelector("#returns-list").textContent = error.message;
    }
  });
  document.querySelector("#returns-mine").click();
}

function renderRelatorios(area) {
  area.innerHTML = `
    <section class="card">
      <h2>Relatório de locações e devoluções</h2>
      <div class="button-row">
        <button id="report-json">Consultar dados</button>
        <button id="report-pdf">Baixar PDF</button>
      </div>
      <div id="report-list"></div>
    </section>
  `;
  document.querySelector("#report-json").addEventListener("click", async () => {
    try {
      document.querySelector("#report-list").innerHTML =
        jsonTable(await api("/api/v1/relatorios/locacoes"));
    } catch (error) {
      document.querySelector("#report-list").textContent = error.message;
    }
  });
  document.querySelector("#report-pdf").addEventListener("click", async () => {
    try {
      const token = getToken();
      const response = await fetch("/api/v1/relatorios/locacoes/pdf", {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      });
      if (!response.ok) throw new Error(`Erro ${response.status}`);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "relatorio-locacoes.pdf";
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      document.querySelector("#report-list").textContent = error.message;
    }
  });
}

if (getToken()) renderDashboard();
else renderLogin();
