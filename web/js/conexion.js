const ESPERA_RECONEXION = 2000;

export function conectar({ alMensaje, alCambiarConexion }) {
  let socket = null;

  function abrir() {
    const protocolo = location.protocol === "https:" ? "wss" : "ws";
    socket = new WebSocket(`${protocolo}://${location.host}/ws`);
    socket.onopen = () => alCambiarConexion(true);
    socket.onmessage = (evento) => alMensaje(JSON.parse(evento.data));
    socket.onclose = () => {
      alCambiarConexion(false);
      setTimeout(abrir, ESPERA_RECONEXION);
    };
  }

  function enviar(mensaje) {
    if (socket && socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify(mensaje));
  }

  abrir();
  return enviar;
}
