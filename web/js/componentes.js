import { BICHOS, bicho, imagenDe } from "./bichos.js";

export function limpiar(texto) {
  return String(texto).replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);
}

export function carta(id, opciones = {}) {
  const clases = ["carta", `es-${id}`];
  if (opciones.elegida) clases.push("carta--elegida");

  const contenido = `<img src="${imagenDe(id)}" alt=""><span class="carta__nombre">${bicho(id).nombre}</span>`;

  if (opciones.accion) {
    return `<button class="${clases.join(" ")}" data-accion="${opciones.accion}" data-indice="${opciones.indice}"
              aria-pressed="${Boolean(opciones.elegida)}" ${opciones.desactivada ? "disabled" : ""}>${contenido}</button>`;
  }
  return `<div class="${clases.join(" ")}" title="${bicho(id).nombre}">${contenido}</div>`;
}

export function reverso() {
  return `<div class="carta carta--reverso" role="img" aria-label="Carta boca abajo"></div>`;
}

export function penalizaciones(lista) {
  const fichas = BICHOS.map((b) => ({ ...b, cantidad: lista.filter((id) => id === b.id).length }))
    .filter((b) => b.cantidad > 0)
    .map(
      (b) => `<span class="ficha es-${b.id} ${b.cantidad >= 3 ? "ficha--peligro" : ""}" title="${b.cantidad} de 4 ${b.plural}">
                <img src="${imagenDe(b.id)}" alt="${b.nombre}">${b.cantidad}/4
              </span>`
    );
  return `<div class="penalizaciones">${fichas.join("") || '<span class="suave pequeno">Sin penalizaciones</span>'}</div>`;
}

export function panelRival(jugador, { enTurno, conectado }) {
  const clases = ["panel", "rival"];
  if (enTurno) clases.push("rival--turno");
  if (!conectado) clases.push("rival--ausente");
  return `
    <article class="${clases.join(" ")}">
      <div class="rival__nombre">
        ${limpiar(jugador.nombre)}
        ${enTurno ? '<span class="etiqueta etiqueta--turno">Su turno</span>' : ""}
        ${conectado ? "" : '<span class="etiqueta etiqueta--aviso">Desconectado</span>'}
      </div>
      <div class="suave pequeno">${jugador.cartasEnMano} ${jugador.cartasEnMano === 1 ? "carta" : "cartas"} en mano</div>
      ${penalizaciones(jugador.penalizaciones)}
    </article>`;
}

export function selectorJugador(titulo, jugadores, elegido) {
  const botones = jugadores.map(
    (j) => `<button class="opcion ${j.id === elegido ? "opcion--elegida" : ""}" data-accion="elegirReceptor" data-id="${j.id}"
              aria-pressed="${j.id === elegido}">${limpiar(j.nombre)}</button>`
  );
  return `<div><div class="opciones__titulo">${titulo}</div><div class="opciones">${botones.join("")}</div></div>`;
}

export function selectorBicho(titulo, elegido) {
  const botones = BICHOS.map(
    (b) => `<button class="opcion ${b.id === elegido ? "opcion--elegida" : ""}" data-accion="elegirDeclaracion" data-bicho="${b.id}"
              aria-pressed="${b.id === elegido}"><img src="${imagenDe(b.id)}" alt="">${b.nombre}</button>`
  );
  return `<div><div class="opciones__titulo">${titulo}</div><div class="opciones">${botones.join("")}</div></div>`;
}
