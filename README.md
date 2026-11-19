# Póquer de Bichos en línea — Avance II (50 %)

Avance al 50 % del proyecto final de Lenguajes de Programación: una adaptación multijugador
del juego de cartas **Póquer de Bichos**. De 4 a 6 personas juegan en tiempo real desde el
navegador: cada una ofrece una carta boca abajo, declara qué bicho es (diciendo la verdad o
mintiendo) y quien la recibe decide si le cree.

## Qué incluye este avance

| Parte | Estado |
|---|---|
| Modelo de datos inmutable (cartas, jugadores, oferta, fases, partida) | Hecho |
| Motor de reglas con funciones puras: mazo, barajar, repartir, ofrecer, responder, penalizar, fin de partida | Hecho |
| Actores: recepción de salas, una sala por partida (bucle principal) y un actor por jugador | Hecho |
| Servidor HTTP y WebSocket con mensajes JSON | Hecho |
| Salas con código de 4 letras, sala de espera y anfitrión | Hecho |
| Cada jugador ve solo su mano; el servidor valida todas las jugadas | Hecho |
| Interfaz web: inicio, sala de espera, partida y pantalla final | Hecho |

## Qué falta para la versión final

| Pendiente | Descripción |
|---|---|
| Mirar y pasar | Quien recibe la carta puede mirarla y pasarla a otro jugador que no la haya visto, manteniendo o cambiando la declaración |
| Reconexión | Volver a la partida al recargar la página o perder la conexión, conservando el lugar y la mano |
| Abandonar, terminar y jugar otra vez | Controles del anfitrión y de los jugadores al terminar o durante la partida |
| Chat de la sala | Mensajes entre los jugadores |
| Reglas en pantalla, enlace de invitación y avisos | Ayudas de la interfaz |
| Despliegue público | Docker y publicación en un hosting gratuito |
| Pruebas automáticas | Pruebas del motor, del servidor y en navegador |

## Tecnologías

| Parte | Tecnología |
|---|---|
| Lenguaje | Scala 3.3.6 |
| Concurrencia | Apache Pekko Typed 1.1.3 (actores y paso de mensajes) |
| Red | Pekko HTTP 1.1.0: WebSocket en `/ws` y archivos estáticos |
| JSON | ujson |
| Interfaz | HTML, CSS y JavaScript sin compilación ni frameworks |

## Para qué se usa cada lenguaje

| Lenguaje | Dónde | Para qué |
|---|---|---|
| Scala 3 | `src/main/scala/` | Todo el juego: datos, reglas, validación de jugadas, reparto al azar, salas, actores, estado y servidor |
| JavaScript | `web/js/` | Solo la pantalla: dibuja el estado que envía el servidor y convierte los clics en mensajes |
| CSS y HTML | `web/css/`, `web/index.html` | Apariencia y página única |

El navegador no decide nada del juego: no baraja, no valida turnos, no calcula quién pierde
y nunca recibe las cartas de los demás.

## Requisitos para ejecutar

- Java 17 o superior (probado con Java 21).
- [sbt](https://www.scala-sbt.org/download) (el proyecto fija sbt 1.10.11 y lo descarga solo).

## Ejecutar

```bash
sbt run
```

Abre `http://localhost:8080`. La variable `PORT` cambia el puerto.

1. Escribe tu nombre y pulsa **Crear sala**.
2. Comparte el código de 4 letras.
3. Los demás escriben su nombre y el código y pulsan **Unirse**.
4. Con 4 a 6 jugadores, el anfitrión pulsa **Iniciar partida**.

Para probar en una sola computadora, abre 4 pestañas: cada pestaña es un jugador distinto.

## Estructura

```
src/main/scala/bichos/
  juego/Modelo.scala          datos inmutables: Bicho, Jugador, Oferta, Resolucion, Fase, Partida, Accion
  juego/Reglas.scala          funciones puras: barajar, repartir, nuevaPartida, jugar
  actores/ActorRecepcion      registro de salas: crea un ActorSala por sala
  actores/ActorSala           bucle principal de una partida: recibe mensajes y guarda el estado
  actores/ActorJugador        un actor por navegador conectado
  red/Json.scala              mensajes JSON y vista filtrada para cada jugador
  red/Servidor.scala          rutas HTTP y unión WebSocket ↔ actor
  Main.scala                  punto de entrada
web/                          interfaz (index.html, css/, js/, img/)
```

## Diagrama de la estructura

```
 Navegador 1 ─┐                     ┌──────────────────────────────┐
 Navegador 2 ─┤  WebSocket (JSON)   │  ActorJugador (uno por cada) │
 Navegador 3 ─┼────────────────────▶│  traduce JSON ↔ mensajes     │
 Navegador 4 ─┘                     └──────────────┬───────────────┘
                                                   │ CrearSala / UnirseASala
                                                   ▼
                                    ┌──────────────────────────────┐
                                    │  ActorRecepcion              │
                                    │  Map[código → ActorSala]     │
                                    └──────────────┬───────────────┘
                                                   │ crea
                                                   ▼
                                    ┌──────────────────────────────┐
     estado filtrado para cada  ◀───│  ActorSala (bucle principal) │
     jugador (solo su mano)         │  Estado inmutable            │
                                    └──────────────┬───────────────┘
                                                   │ llama
                                                   ▼
                                    ┌──────────────────────────────┐
                                    │  Reglas.jugar (función pura) │
                                    │  Partida → Either[error,     │
                                    │            Partida nueva]    │
                                    └──────────────────────────────┘
```

## Estrategia de concurrencia

- Cada navegador conectado tiene su propio `ActorJugador`; cada sala tiene un `ActorSala`
  que es el bucle principal de la partida; `ActorRecepcion` crea las salas y dirige a los
  jugadores. Se comunican solo con mensajes.
- Un actor procesa un mensaje a la vez. Si dos jugadores actúan al mismo tiempo, sus
  mensajes llegan al buzón del `ActorSala` y se atienden en orden: no hay variables
  compartidas ni candados.
- El estado es un valor inmutable. `ActorSala` calcula el estado nuevo con funciones puras
  y continúa con él (`activa(estadoNuevo)`). Si la jugada no es válida, `Reglas.jugar`
  devuelve `Left(mensaje)`, el estado no cambia y solo quien la hizo recibe el error.
- Tras cada cambio, `ActorSala` envía a cada jugador su propia vista: la parte pública es la
  misma para todos y la mano solo la recibe su dueño. Así todos ven en tiempo real el turno,
  las cartas de cada uno, las penalizaciones y cada jugada.

## Principales funciones

| Función | Archivo | Qué hace |
|---|---|---|
| `Reglas.barajar(cartas, semilla)` | `Reglas.scala` | Mezcla el mazo; la misma semilla da siempre el mismo orden |
| `Reglas.repartir(cartas, participantes)` | `Reglas.scala` | Reparte todas las cartas de una en una |
| `Reglas.nuevaPartida(participantes, semilla)` | `Reglas.scala` | Valida de 4 a 6 jugadores, reparte y elige al azar quién empieza |
| `Reglas.jugar(partida, jugador, accion)` | `Reglas.scala` | Aplica `Ofrecer` o `Responder` y devuelve la partida nueva o un error |
| `ActorSala.agregarMiembro` | `ActorSala.scala` | Valida nombre, cupo y que la sala no esté jugando |
| `ActorSala.iniciarPartida` | `ActorSala.scala` | Solo el anfitrión; crea la partida con los miembros de la sala |
| `ActorSala.aplicarJugada` | `ActorSala.scala` | Une la sala con el motor: busca al jugador y llama a `Reglas.jugar` |
| `Json.leerComando` | `Json.scala` | Convierte el texto del navegador en un `Comando` de Scala |
| `Json.estado` / `Json.vistaPartida` | `Json.scala` | Arman la vista de cada jugador sin cartas ajenas |

## Requisitos del trabajo y cómo se cumplen en este avance

| Requisito del enunciado | Cómo se cumple |
|---|---|
| Mínimo 4 jugadores simultáneos | De 4 a 6 por sala; con menos de 4 el servidor no inicia la partida |
| Lenguaje funcional (Scala) | Toda la lógica, el estado y la concurrencia están en Scala; la pantalla usa HTML por WebSocket, como permite el enunciado |
| Datos inmutables | `case class`, `enum`, `Vector`, `Map` y tuplas; no hay ningún `var` en `src/main` |
| Funciones puras | El motor (`Reglas.scala`) no modifica nada: devuelve una partida nueva o un error; el azar entra como parámetro |
| Al menos 3 conexiones con estado coherente | Un único estado por sala dentro de `ActorSala`, enviado a todos tras cada cambio |
| Actores por jugador y bucle principal | `ActorJugador` por conexión y `ActorSala` como bucle principal (Pekko Typed) |
| Paso de mensajes | Los actores solo se comunican con mensajes; con el navegador, JSON por WebSocket |
| Ver a los otros jugadores en tiempo real | Turno, cantidad de cartas, penalizaciones, conexión y cada jugada aparecen en todas las pantallas sin recargar |

## Reglas implementadas en este avance

64 cartas (8 bichos × 8). Se reparten todas. En su turno, un jugador ofrece una carta boca
abajo a otro y declara qué bicho es. Quien la recibe responde *verdad* o *mentira*. Si
acierta, la carta es para quien se la ofreció; si falla, para él. El penalizado deja la
carta boca arriba frente a sí e inicia la ronda siguiente. Pierde quien reúne 4 cartas del
mismo bicho o quien debe iniciar una ronda sin cartas; los demás ganan.

## Recursos

- Dibujos de los bichos (`web/img/bichos/`): [Twemoji](https://github.com/jdecked/twemoji),
  © Twitter, Inc. y otros colaboradores, licencia [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
  No hay icono de chinche: se usa el del escarabajo.
- Reverso de la carta e icono: dibujos originales del proyecto.
- Tipografías Figtree y Fredoka, de Google Fonts, licencia SIL Open Font License.
