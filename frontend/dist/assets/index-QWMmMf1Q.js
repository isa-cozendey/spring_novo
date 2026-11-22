(function(){const o=document.createElement("link").relList;if(o&&o.supports&&o.supports("modulepreload"))return;for(const a of document.querySelectorAll('link[rel="modulepreload"]'))t(a);new MutationObserver(a=>{for(const n of a)if(n.type==="childList")for(const c of n.addedNodes)c.tagName==="LINK"&&c.rel==="modulepreload"&&t(c)}).observe(document,{childList:!0,subtree:!0});function e(a){const n={};return a.integrity&&(n.integrity=a.integrity),a.referrerPolicy&&(n.referrerPolicy=a.referrerPolicy),a.crossOrigin==="use-credentials"?n.credentials="include":a.crossOrigin==="anonymous"?n.credentials="omit":n.credentials="same-origin",n}function t(a){if(a.ep)return;a.ep=!0;const n=e(a);fetch(a.href,n)}})();const v=document.querySelector("#app"),h="";function m(){return localStorage.getItem("bikes_token")}function d(){const r=m();if(!r)return null;try{const o=r.split(".")[1].replace(/-/g,"+").replace(/_/g,"/");return JSON.parse(decodeURIComponent(atob(o).split("").map(t=>"%"+("00"+t.charCodeAt(0).toString(16)).slice(-2)).join(""))).role||null}catch{return null}}async function i(r,o={}){const e={"Content-Type":"application/json",...o.headers||{}},t=m();t&&(e.Authorization=`Bearer ${t}`);const a=await fetch(`${h}${r}`,{...o,headers:e}),n=await a.text();let c=null;if(n)try{c=JSON.parse(n)}catch{c=n}if(!a.ok)throw new Error(c?.message||c?.errors?.[0]?.message||c||`Erro ${a.status}`);return c}function b(r){return r==null?"":String(r).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;")}function S(r){if(!r)return"-";const o=new Date(r);return Number.isNaN(o.getTime())?r:o.toLocaleString("pt-BR")}function s(r){if(!Array.isArray(r)||!r.length)return'<p class="empty">Nenhum registro encontrado.</p>';const o=Object.keys(r[0]).filter(e=>!["password","codigoSeguranca","numero"].includes(e));return`<div class="table-wrap"><table><thead><tr>${o.map(e=>`<th>${b(e)}</th>`).join("")}</tr></thead><tbody>${r.map(e=>`<tr>${o.map(t=>`<td>${b(typeof e[t]=="object"?JSON.stringify(e[t]):t.toLowerCase().includes("data")||t.toLowerCase().includes("hora")?S(e[t]):e[t])}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`}function l(r="result"){return`<pre id="${r}" class="result">Selecione uma ação.</pre>`}function f(r){v.innerHTML=`
    <header>
      <div>
        <h1>Bikes</h1>
        <p>Painel de testes dos recursos da API</p>
      </div>
      <div class="auth-summary">
        <span>${b(d()||"Sem sessão")}</span>
        <button id="logout" class="danger">Sair</button>
      </div>
    </header>
    ${r}
  `,document.querySelector("#logout")?.addEventListener("click",()=>{localStorage.removeItem("bikes_token"),y()})}function y(){f(`
    <main class="login-grid">
      <section class="card">
        <h2>Entrar</h2>
        <form id="login-form">
          <label>E-mail<input name="username" type="email" required value="maria@gmail.com"></label>
          <label>Senha (6 caracteres)<input name="password" type="password" minlength="6" maxlength="6" required value="123456"></label>
          <button type="submit">Entrar</button>
        </form>
        ${l("login-result")}
      </section>
      <section class="card">
        <h2>Cadastrar usuário</h2>
        <form id="register-form">
          <label>E-mail<input name="username" type="email" required></label>
          <label>Senha (6 caracteres)<input name="password" type="password" minlength="6" maxlength="6" required></label>
          <button type="submit">Cadastrar</button>
        </form>
        ${l("register-result")}
      </section>
    </main>
  `),document.querySelector("#login-form").addEventListener("submit",async r=>{r.preventDefault();const o=Object.fromEntries(new FormData(r.currentTarget));try{const e=await i("/api/v1/auth",{method:"POST",body:JSON.stringify(o)});localStorage.setItem("bikes_token",e.token),g()}catch(e){document.querySelector("#login-result").textContent=e.message}}),document.querySelector("#register-form").addEventListener("submit",async r=>{r.preventDefault();const o=Object.fromEntries(new FormData(r.currentTarget));try{const e=await i("/api/v1/usuarios",{method:"POST",body:JSON.stringify(o)});document.querySelector("#register-result").textContent=`Usuário criado: ${JSON.stringify(e,null,2)}`}catch(e){document.querySelector("#register-result").textContent=e.message}})}function g(){const o=d()==="ROLE_ADMIN";f(`
    <nav>
      ${["Clientes","Bicicletas","Locações","Devoluções",...o?["Relatórios"]:[]].map((e,t)=>`<button class="tab ${t===0?"active":""}" data-tab="${e}">${e}</button>`).join("")}
    </nav>
    <main id="dashboard"></main>
  `),document.querySelectorAll(".tab").forEach(e=>{e.addEventListener("click",()=>{document.querySelectorAll(".tab").forEach(t=>t.classList.remove("active")),e.classList.add("active"),p(e.dataset.tab)})}),p("Clientes")}function p(r){const o=document.querySelector("#dashboard");r==="Clientes"&&q(o),r==="Bicicletas"&&L(o),r==="Locações"&&E(o),r==="Devoluções"&&O(o),r==="Relatórios"&&C(o)}async function u(r,o,e){try{const t=await i(r,o);document.querySelector("#"+e).textContent=JSON.stringify(t,null,2)}catch(t){document.querySelector("#"+e).textContent=t.message}}function q(r){const o=d()==="ROLE_ADMIN";r.innerHTML=`
    <section class="grid">
      <article class="card">
        <h2>Cadastrar cliente</h2>
        <form id="client-form">
          <label>Nome<input name="nome" required></label>
          <label>CPF (11 dígitos)<input name="cpf" minlength="11" maxlength="14" required></label>
          <button>Cadastrar</button>
        </form>
        ${l("client-result")}
      </article>
      <article class="card">
        <h2>Consultas</h2>
        <div class="button-row">
          <button id="client-me">Meus dados</button>
          ${o?'<button id="clients-all">Todos os clientes</button>':""}
        </div>
        ${l("client-list")}
      </article>
    </section>
  `,document.querySelector("#client-form").addEventListener("submit",e=>{e.preventDefault(),u("/api/v1/clientes",{method:"POST",body:JSON.stringify(Object.fromEntries(new FormData(e.currentTarget)))},"client-result")}),document.querySelector("#client-me").addEventListener("click",async()=>{try{document.querySelector("#client-list").textContent=JSON.stringify(await i("/api/v1/clientes/me"),null,2)}catch(e){document.querySelector("#client-list").textContent=e.message}}),document.querySelector("#clients-all")?.addEventListener("click",async()=>{try{const e=await i("/api/v1/clientes");document.querySelector("#client-list").innerHTML=s(e)}catch(e){document.querySelector("#client-list").textContent=e.message}})}function L(r){const o=d()==="ROLE_ADMIN";r.innerHTML=o?`
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
        ${l("bike-create")}
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
        ${l("bike-update-result")}
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
  `:'<section class="card"><h2>Bicicletas</h2><p>Somente administradores podem gerir bicicletas.</p></section>',o&&(document.querySelector("#bike-form").addEventListener("submit",e=>{e.preventDefault();const t=Object.fromEntries(new FormData(e.currentTarget));u("/api/v1/bicicletas",{method:"POST",body:JSON.stringify(t)},"bike-create")}),document.querySelector("#bike-update").addEventListener("submit",e=>{e.preventDefault();const t=Object.fromEntries(new FormData(e.currentTarget));u(`/api/v1/bicicletas/${encodeURIComponent(t.codigoAtual)}`,{method:"PUT",body:JSON.stringify({codigo:t.codigo,status:t.status})},"bike-update-result")}),document.querySelector("#bike-delete").addEventListener("click",()=>{const e=document.querySelector('[name="codigoAtual"]').value;e&&i(`/api/v1/bicicletas/${encodeURIComponent(e)}`,{method:"DELETE"}).then(()=>document.querySelector("#bike-update-result").textContent="Bicicleta removida.").catch(t=>document.querySelector("#bike-update-result").textContent=t.message)}),document.querySelector("#bike-list").addEventListener("click",async()=>{try{document.querySelector("#bike-list-result").innerHTML=s(await i("/api/v1/bicicletas"))}catch(e){document.querySelector("#bike-list-result").textContent=e.message}}),document.querySelector("#bike-search").addEventListener("submit",async e=>{e.preventDefault();const{codigo:t}=Object.fromEntries(new FormData(e.currentTarget));try{document.querySelector("#bike-list-result").innerHTML=s([await i(`/api/v1/bicicletas/${encodeURIComponent(t)}`)])}catch(a){document.querySelector("#bike-list-result").textContent=a.message}}),document.querySelector("#bike-list").click())}function E(r){const o=d()==="ROLE_ADMIN";r.innerHTML=`
    <section class="grid">
      <article class="card">
        <h2>Solicitar aluguel</h2>
        <form id="rent-form">
          <label>Devolução prevista<input name="dataHoraPrevistaDevolucao" type="datetime-local" required></label>
          <button>Solicitar bicicleta livre</button>
        </form>
        ${l("rent-result")}
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
        ${l("effect-result")}
      </article>
    </section>
    <section class="card">
      <div class="button-row"><h2>Locações</h2>
        <button id="locations-mine">Minhas locações</button>
        ${o?'<button id="locations-all">Todas</button>':""}
      </div>
      <div id="locations-list"></div>
    </section>
  `,document.querySelector("#rent-form").addEventListener("submit",e=>{e.preventDefault();const t=Object.fromEntries(new FormData(e.currentTarget));u("/api/v1/locacoes/solicitacoes",{method:"POST",body:JSON.stringify({dataHoraPrevistaDevolucao:`${t.dataHoraPrevistaDevolucao}:00`})},"rent-result")}),document.querySelector("#effect-form").addEventListener("submit",e=>{e.preventDefault();const t=Object.fromEntries(new FormData(e.currentTarget));u(`/api/v1/locacoes/${t.id}/efetivar`,{method:"POST",body:JSON.stringify({cartao:{titular:t.titular,numero:t.numero,validade:t.validade,codigoSeguranca:t.codigoSeguranca}})},"effect-result")}),document.querySelector("#locations-mine").addEventListener("click",async()=>{try{document.querySelector("#locations-list").innerHTML=s(await i("/api/v1/locacoes/me"))}catch(e){document.querySelector("#locations-list").textContent=e.message}}),document.querySelector("#locations-all")?.addEventListener("click",async()=>{try{document.querySelector("#locations-list").innerHTML=s(await i("/api/v1/locacoes"))}catch(e){document.querySelector("#locations-list").textContent=e.message}}),document.querySelector("#locations-mine").click()}function O(r){const o=d()==="ROLE_ADMIN";r.innerHTML=`
    <section class="grid">
      <article class="card">
        <h2>Registrar devolução</h2>
        <form id="return-form">
          <label>ID da locação<input name="locacaoId" type="number" required></label>
          <label><input name="problema" type="checkbox"> Problema identificado</label>
          <label>Observação<textarea name="observacao" maxlength="500"></textarea></label>
          <button>Registrar devolução</button>
        </form>
        ${l("return-result")}
      </article>
      <article class="card">
        <h2>Alterar bicicleta da devolução</h2>
        ${o?`
          <form id="return-status-form">
            <label>ID da devolução<input name="devolucaoId" type="number" required></label>
            <label>Status<select name="status"><option>LIVRE</option><option>EM_MANUTENCAO</option></select></label>
            <button>Alterar</button>
          </form>
          ${l("return-status-result")}
        `:"<p>Somente administradores podem alterar o status.</p>"}
      </article>
    </section>
    <section class="card">
      <div class="button-row"><h2>Devoluções</h2>
        <button id="returns-mine">Minhas devoluções</button>
        ${o?'<button id="returns-all">Todas</button>':""}
      </div>
      <div id="returns-list"></div>
    </section>
  `,document.querySelector("#return-form").addEventListener("submit",e=>{e.preventDefault();const t=Object.fromEntries(new FormData(e.currentTarget));u(`/api/v1/devolucoes/locacao/${t.locacaoId}`,{method:"POST",body:JSON.stringify({problemaIdentificado:t.problema==="on",observacao:t.observacao||null})},"return-result")}),document.querySelector("#return-status-form")?.addEventListener("submit",e=>{e.preventDefault();const t=Object.fromEntries(new FormData(e.currentTarget));u(`/api/v1/devolucoes/${t.devolucaoId}/bicicleta-status?status=${t.status}`,{method:"PATCH"},"return-status-result")}),document.querySelector("#returns-mine").addEventListener("click",async()=>{try{document.querySelector("#returns-list").innerHTML=s(await i("/api/v1/devolucoes/me"))}catch(e){document.querySelector("#returns-list").textContent=e.message}}),document.querySelector("#returns-all")?.addEventListener("click",async()=>{try{document.querySelector("#returns-list").innerHTML=s(await i("/api/v1/devolucoes"))}catch(e){document.querySelector("#returns-list").textContent=e.message}}),document.querySelector("#returns-mine").click()}function C(r){r.innerHTML=`
    <section class="card">
      <h2>Relatório de locações e devoluções</h2>
      <div class="button-row">
        <button id="report-json">Consultar dados</button>
        <button id="report-pdf">Baixar PDF</button>
      </div>
      <div id="report-list"></div>
    </section>
  `,document.querySelector("#report-json").addEventListener("click",async()=>{try{document.querySelector("#report-list").innerHTML=s(await i("/api/v1/relatorios/locacoes"))}catch(o){document.querySelector("#report-list").textContent=o.message}}),document.querySelector("#report-pdf").addEventListener("click",async()=>{try{const o=m(),e=await fetch("/api/v1/relatorios/locacoes/pdf",{headers:o?{Authorization:`Bearer ${o}`}:{}});if(!e.ok)throw new Error(`Erro ${e.status}`);const t=await e.blob(),a=URL.createObjectURL(t),n=document.createElement("a");n.href=a,n.download="relatorio-locacoes.pdf",n.click(),URL.revokeObjectURL(a)}catch(o){document.querySelector("#report-list").textContent=o.message}})}m()?g():y();
