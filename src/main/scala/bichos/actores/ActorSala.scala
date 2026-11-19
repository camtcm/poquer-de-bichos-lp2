package bichos.actores

import bichos.juego.*
import bichos.red.Json
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import scala.util.Random

object ActorSala:

  type RefJugador = ActorRef[ActorJugador.Mensaje]

  enum Mensaje:
    case Unirse(nombre: String, jugador: RefJugador)
    case Desconectar(jugador: RefJugador)
    case Iniciar(jugador: RefJugador)
    case Jugar(jugador: RefJugador, accion: Accion)
  import Mensaje.*

  case class Miembro(id: String, nombre: String, conexion: Option[RefJugador])

  case class Estado(codigo: String, miembros: Vector[Miembro], partida: Option[Partida], creados: Int):
    def conectados: Vector[Miembro] = miembros.filter(_.conexion.isDefined)
    def anfitrion: Option[String]   = conectados.headOption.map(_.id)

    def miembroDe(jugador: RefJugador): Option[Miembro] = miembros.find(_.conexion.contains(jugador))
    def esAnfitrion(jugador: RefJugador): Boolean       = miembroDe(jugador).map(_.id) == anfitrion

  val largoMaximoNombre = 16

  def apply(codigo: String): Behavior[Mensaje] = activa(Estado(codigo, Vector.empty, None, 0))

  private def activa(estado: Estado): Behavior[Mensaje] =
    Behaviors.receive { (contexto, mensaje) =>
      mensaje match
        case Unirse(nombre, jugador) =>
          agregarMiembro(estado, nombre, jugador) match
            case Left(motivo) =>
              rechazar(jugador, motivo)
              if estado.miembros.isEmpty then Behaviors.stopped else Behaviors.same
            case Right(nuevo) =>
              jugador ! ActorJugador.Mensaje.EntroASala(contexto.self)
              continuar(nuevo)

        case Desconectar(jugador) =>
          val nuevo = quitarConexion(estado, jugador)
          if nuevo.conectados.isEmpty then Behaviors.stopped else continuar(nuevo)

        case Iniciar(jugador) =>
          iniciarPartida(estado, jugador, Random.nextLong()) match
            case Right(nuevo) => continuar(nuevo)
            case Left(motivo) =>
              rechazar(jugador, motivo)
              Behaviors.same

        case Jugar(jugador, accion) =>
          aplicarJugada(estado, jugador, accion) match
            case Right(nuevo) => continuar(nuevo)
            case Left(motivo) =>
              rechazar(jugador, motivo)
              Behaviors.same
    }

  private def continuar(estado: Estado): Behavior[Mensaje] =
    difundir(estado)
    activa(estado)

  private def difundir(estado: Estado): Unit =
    val participantes = estado.miembros.map(m => Json.Participante(m.id, m.nombre, m.conexion.isDefined))
    for
      miembro  <- estado.miembros
      conexion <- miembro.conexion
    do
      conexion ! ActorJugador.Mensaje.Enviar(
        Json.estado(estado.codigo, participantes, estado.anfitrion, estado.partida, para = miembro.id)
      )

  private def rechazar(jugador: RefJugador, motivo: String): Unit =
    jugador ! ActorJugador.Mensaje.Enviar(Json.error(motivo))

  def agregarMiembro(estado: Estado, nombre: String, jugador: RefJugador): Either[String, Estado] =
    val limpio = nombre.trim
    if limpio.isEmpty then Left("Escribe tu nombre.")
    else if limpio.length > largoMaximoNombre then Left(s"El nombre puede tener hasta $largoMaximoNombre letras.")
    else if estado.partida.isDefined then Left("Esa sala ya está jugando una partida.")
    else if estado.miembros.exists(_.nombre.equalsIgnoreCase(limpio)) then Left("Ya hay alguien con ese nombre en la sala.")
    else if estado.miembros.size >= Reglas.maxJugadores then Left("La sala está llena.")
    else
      val miembro = Miembro(s"j${estado.creados + 1}", limpio, Some(jugador))
      Right(estado.copy(miembros = estado.miembros :+ miembro, creados = estado.creados + 1))

  def quitarConexion(estado: Estado, jugador: RefJugador): Estado =
    if estado.partida.isDefined then
      estado.copy(miembros = estado.miembros.map(m => if m.conexion.contains(jugador) then m.copy(conexion = None) else m))
    else estado.copy(miembros = estado.miembros.filterNot(_.conexion.contains(jugador)))

  def iniciarPartida(estado: Estado, jugador: RefJugador, semilla: Long): Either[String, Estado] =
    if !estado.esAnfitrion(jugador) then Left("Solo el anfitrión puede iniciar la partida.")
    else if estado.partida.isDefined then Left("Ya hay una partida en curso.")
    else
      Reglas
        .nuevaPartida(estado.miembros.map(m => (m.id, m.nombre)), semilla)
        .map(partida => estado.copy(partida = Some(partida)))

  def aplicarJugada(estado: Estado, jugador: RefJugador, accion: Accion): Either[String, Estado] =
    (estado.miembroDe(jugador), estado.partida) match
      case (None, _) => Left("No estás en esta sala.")
      case (_, None) => Left("La partida todavía no empezó.")
      case (Some(miembro), Some(partida)) =>
        Reglas.jugar(partida, miembro.id, accion).map(nueva => estado.copy(partida = Some(nueva)))
