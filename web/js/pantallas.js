import { bicho } from "./bichos.js";
import { carta, reverso, penalizaciones, panelRival, selectorJugador, selectorBicho, limpiar } from "./componentes.js";

const MIN_JUGADORES = 4;
const MAX_JUGADORES = 6;

export function dibujar(app) {
  const pantalla = !app.sala ? pantallaInicio(app) : !app.sala.partida ? pantallaSala(app) : app.sala.partida.final ? pantallaFinal(app) : pantallaPartida(app);
  return `
    ${app.conectado ? "" : '<div class="sin-conexion" role="status">Sin conexión. Reconectando…</div>'}
    ${pantalla}
    ${app.aviso ? `<div class="aviso" role="alert">${limpiar(app.aviso)}</div>` : ""}`;
}

function pantallaInicio(app) {
  const { nombre, codigo } = app.ui;
  return `
    <div class="inicio">
      <section>
        <p class="inicio__marca">Póquer de Bichos · Avance 2</p>
        <h1>¿Verdad o mentira?</h1>
        <p class="suave">Ofrece una carta boca abajo, di qué bicho es y deja que los demás decidan si te creen. De 4 a 6 jugadores.</p>
        <div class="inicio__cartas" aria-hidden="true">
          ${carta("sapo")}${carta("arana")}${carta("murcielago")}${reverso()}
        </div>
      </section>

      <section class="panel formulario">
        <div>
          <label for="nombre">Tu nombre</label>
          <input class="campo" id="nombre" data-campo="nombre" value="${limpiar(nombre)}" maxlength="16" autocomplete="nickname" placeholder="Por ejemplo, Ana">
        </div>
        <button class="boton boton--primario" data-accion="crear" ${app.conectado ? "" : "disabled"}>Crear sala</button>
        <div class="separador">o únete a una sala</div>
        <div>
          <label for="codigo">Código de la sala</label>
          <div class="unirse">
            <input class="campo campo--codigo" id="codigo" data-campo="codigo" value="${limpiar(codigo)}" maxlength="4" autocomplete="off" placeholder="ABCD">
            <button class="boton" data-accion="unirse" ${app.conectado ? "" : "disabled"}>Unirse</button>
          </div>
        </div>
      </section>
    </div>`;
}

function pantallaSala(app) {
  const { sala } = app;
  const soyAnfitrion = sala.anfitrion === sala.tuId;
  const faltan = MIN_JUGADORES - sala.miembros.length;
  const lugaresLibres = MAX_JUGADORES - sala.miembros.length;

  const jugadores = sala.miembros.map(
    (m) => `
      <li class="panel">
        <span class="avatar">${limpiar(m.nombre.charAt(0).toUpperCase())}</span>
        <strong>${limpiar(m.nombre)}</strong>
        ${m.id === sala.tuId ? '<span class="etiqueta">Tú</span>' : ""}
        ${m.id === sala.anfitrion ? '<span class="etiqueta">Anfitrión</span>' : ""}
      </li>`
  );
  const vacios = Array(lugaresLibres).fill('<li class="panel vacio">Lugar libre</li>');

  return `
    <div class="sala">
      <h1>Sala de espera</h1>
      <section>
        <h2>Jugadores <span class="suave pequeno">${sala.miembros.length} de ${MAX_JUGADORES}</span></h2>
        <ul class="lista-jugadores">${jugadores.join("")}${vacios.join("")}</ul>
      </section>

      <aside class="panel">
        <p class="suave pequeno">Código de la sala</p>
        <p class="sala__codigo">${sala.codigo}</p>
        <p class="suave pequeno">Comparte el código para que tus amigos entren.</p>
        <div class="sala__acciones">
          ${
            soyAnfitrion
              ? `<button class="boton boton--primario" data-accion="iniciar" ${faltan > 0 ? "disabled" : ""}>Iniciar partida</button>`
              : '<p class="suave">El anfitrión iniciará la partida.</p>'
          }
          ${faltan > 0 ? `<p class="suave pequeno">${faltan === 1 ? "Falta 1 jugador" : `Faltan ${faltan} jugadores`} para empezar.</p>` : ""}
        </div>
      </aside>
    </div>`;
}

function pantallaPartida(app) {
  const { sala, ui } = app;
  const partida = sala.partida;
  const yo = partida.jugadores.find((j) => j.id === sala.tuId);
  const conectado = (id) => sala.miembros.some((m) => m.id === id && m.conectado);
  const meTocaOfrecer = partida.fase === "oferta" && partida.turno === sala.tuId;

  const rivales = partida.jugadores
    .filter((j) => j.id !== sala.tuId)
    .map((j) => panelRival(j, { enTurno: partida.turno === j.id, conectado: conectado(j.id) }));

  const mano = partida.tuMano.map((id, indice) =>
    carta(id, { accion: "elegirCarta", indice, elegida: meTocaOfrecer && ui.carta === indice, desactivada: !meTocaOfrecer })
  );

  return `
    <div class="partida">
      <header class="barra">
        <span class="barra__titulo">Póquer de Bichos</span>
        <span class="etiqueta">Sala ${sala.codigo}</span>
        <span class="etiqueta">Ronda ${partida.ronda}</span>
      </header>

      <section class="rivales" aria-label="Otros jugadores">${rivales.join("")}</section>

      <section class="mesa" aria-live="polite">${mesa(app)}</section>

      <aside class="panel ultima" aria-label="Última jugada">${ultimaJugada(partida)}</aside>

      <section class="mi-zona">
        <div class="mi-zona__cabecera">
          <h3>${limpiar(yo.nombre)} <span class="suave pequeno">(tú)</span></h3>
          <span class="etiqueta">${partida.tuMano.length} cartas en mano</span>
          ${penalizaciones(yo.penalizaciones)}
        </div>
        <div class="mano" id="mano">${mano.join("")}</div>
      </section>
    </div>`;
}

function mesa(app) {
  const { sala, ui } = app;
  const partida = sala.partida;
  const nombre = (id) => limpiar(partida.jugadores.find((j) => j.id === id).nombre);
  const otros = partida.jugadores.filter((j) => j.id !== sala.tuId);

  if (partida.fase === "oferta") {
    if (partida.turno !== sala.tuId) {
      return `<h2>Espera tu turno</h2><p>${nombre(partida.turno)} está eligiendo una carta para ofrecer.</p>`;
    }
    if (ui.carta === null) {
      return `<h2>Tu turno</h2><p>Elige una carta de tu mano para ofrecerla.</p>`;
    }
    const listo = ui.receptor && ui.declaracion;
    return `
      <h2>Prepara tu oferta</h2>
      <div class="mesa__contenido">
        ${carta(partida.tuMano[ui.carta])}
        <div class="mesa__texto">
          ${selectorJugador("¿A quién se la ofreces?", otros, ui.receptor)}
          ${selectorBicho("¿Qué vas a decir que es? Puedes mentir.", ui.declaracion)}
          <div class="fila">
            <button class="boton boton--primario" data-accion="ofrecer" ${listo ? "" : "disabled"}>
              ${listo ? `Ofrecer a ${nombre(ui.receptor)} diciendo «${bicho(ui.declaracion).una}»` : "Ofrecer carta"}
            </button>
          </div>
        </div>
      </div>`;
  }

  const oferta = partida.oferta;
  const dice = `«Es ${bicho(oferta.declaracion).una}»`;

  if (oferta.receptor !== sala.tuId) {
    return `
      <h2>Espera tu turno</h2>
      <div class="mesa__contenido">
        ${oferta.carta ? carta(oferta.carta) : reverso()}
        <div class="mesa__texto">
          <p>${nombre(oferta.oferente)} le ofrece una carta a ${nombre(oferta.receptor)} y dice:</p>
          <p class="cita">${dice}</p>
          <p>${nombre(oferta.receptor)} está decidiendo si le cree.</p>
        </div>
      </div>`;
  }

  return `
    <h2>¿Le crees a ${nombre(oferta.oferente)}?</h2>
    <div class="mesa__contenido">
      ${reverso()}
      <div class="mesa__texto">
        <p>${nombre(oferta.oferente)} te ofrece esta carta y dice:</p>
        <p class="cita">${dice}</p>
        <div class="fila">
          <button class="boton boton--verdad" data-accion="responder" data-valor="verdad">Es verdad</button>
          <button class="boton boton--mentira" data-accion="responder" data-valor="mentira">Es mentira</button>
        </div>
      </div>
    </div>`;
}

function ultimaJugada(partida) {
  const r = partida.ultima;
  if (!r) return `<div class="ultima__texto"><h3>Última jugada</h3><p class="suave">Todavía no hay jugadas.</p></div>`;
  const nombre = (id) => limpiar(partida.jugadores.find((j) => j.id === id).nombre);
  return `
    ${carta(r.carta)}
    <div class="ultima__texto">
      <h3>Última jugada</h3>
      <p>${nombre(r.oferente)} dijo «${bicho(r.declaracion).una}» y era <strong>${bicho(r.carta).una}</strong>.</p>
      <p>${nombre(r.receptor)} respondió «${r.dijoVerdad ? "verdad" : "mentira"}» y ${r.acerto ? "acertó" : "se equivocó"}.</p>
      <p><strong>${nombre(r.penalizado)}</strong> se queda la carta.</p>
    </div>`;
}

function pantallaFinal(app) {
  const { sala } = app;
  const partida = sala.partida;
  const fin = partida.final;
  const perdedor = partida.jugadores.find((j) => j.id === fin.perdedor);
  const perdi = fin.perdedor === sala.tuId;
  const sujeto = perdi ? "Reuniste" : `${limpiar(perdedor.nombre)} reunió`;

  const motivo =
    fin.motivo === "cuatroIguales"
      ? `${sujeto} cuatro ${bicho(fin.bicho).plural}.`
      : perdi
        ? "Te tocaba iniciar la ronda y no te quedaban cartas."
        : `A ${limpiar(perdedor.nombre)} le tocaba iniciar la ronda y no le quedaban cartas.`;

  const resumen = partida.jugadores.map(
    (j) => `<div><strong>${limpiar(j.nombre)}</strong>${j.id === fin.perdedor ? '<span class="etiqueta etiqueta--aviso">Perdió</span>' : ""}${penalizaciones(j.penalizaciones)}</div>`
  );

  return `
    <div class="panel final">
      <p class="suave pequeno">PARTIDA TERMINADA</p>
      <h1>${perdi ? "Perdiste" : "¡Ganaste!"}</h1>
      <p>${motivo}</p>
      <div class="final__resumen">${resumen.join("")}</div>
      <button class="boton" data-accion="volverAlInicio">Volver al inicio</button>
    </div>`;
}
