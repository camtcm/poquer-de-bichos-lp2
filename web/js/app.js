import { conectar } from "./conexion.js";
import { dibujar } from "./pantallas.js";

const raiz = document.getElementById("app");
const TIEMPO_AVISO = 4000;

const app = {
  conectado: false,
  sala: null,
  aviso: null,
  ui: {
    nombre: "",
    codigo: "",
    carta: null,
    receptor: null,
    declaracion: null,
  },
};

const enviar = conectar({
  alCambiarConexion(conectado) {
    app.conectado = conectado;
    if (!conectado && app.sala) {
      app.sala = null;
      mostrarAviso("Se perdió la conexión con la sala.");
    }
    render();
  },

  alMensaje(mensaje) {
    if (mensaje.tipo === "estado") recibirEstado(mensaje);
    if (mensaje.tipo === "error") mostrarAviso(mensaje.mensaje);
    render();
  },
});

function recibirEstado(nuevo) {
  if (momento(app.sala?.partida) !== momento(nuevo.partida)) {
    app.ui.carta = null;
    app.ui.receptor = null;
    app.ui.declaracion = null;
  }
  if (!app.sala) app.aviso = null;
  app.sala = nuevo;
}

function momento(partida) {
  if (!partida) return "sin partida";
  return [partida.ronda, partida.fase, partida.turno].join("|");
}

let temporizadorAviso = null;
function mostrarAviso(texto) {
  app.aviso = texto;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => {
    app.aviso = null;
    render();
  }, TIEMPO_AVISO);
}

const acciones = {
  crear: () => enviar({ tipo: "crear", nombre: app.ui.nombre }),
  unirse: () => enviar({ tipo: "unirse", codigo: app.ui.codigo, nombre: app.ui.nombre }),
  iniciar: () => enviar({ tipo: "iniciar" }),

  elegirCarta: (datos) => (app.ui.carta = Number(datos.indice)),
  elegirReceptor: (datos) => (app.ui.receptor = datos.id),
  elegirDeclaracion: (datos) => (app.ui.declaracion = datos.bicho),

  ofrecer: () =>
    enviar({
      tipo: "ofrecer",
      carta: app.sala.partida.tuMano[app.ui.carta],
      receptor: app.ui.receptor,
      declaracion: app.ui.declaracion,
    }),
  responder: (datos) => enviar({ tipo: "responder", esVerdad: datos.valor === "verdad" }),

  volverAlInicio: () => location.reload(),
};

raiz.addEventListener("click", (evento) => {
  const elemento = evento.target.closest("[data-accion]");
  if (!elemento || elemento.disabled) return;
  acciones[elemento.dataset.accion](elemento.dataset);
  render();
});

raiz.addEventListener("input", (evento) => {
  const campo = evento.target.dataset.campo;
  if (campo) app.ui[campo] = evento.target.value;
});

raiz.addEventListener("keydown", (evento) => {
  const campo = evento.target.dataset.campo;
  if (evento.key !== "Enter" || !campo) return;
  acciones[campo === "codigo" ? "unirse" : "crear"]();
});

function render() {
  const campoActivo = document.activeElement?.id;
  const desplazamiento = document.getElementById("mano")?.scrollLeft ?? 0;

  raiz.innerHTML = dibujar(app);

  if (campoActivo) document.getElementById(campoActivo)?.focus();
  const mano = document.getElementById("mano");
  if (mano) mano.scrollLeft = desplazamiento;
}

render();
