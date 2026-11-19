package bichos.actores

import bichos.red.Json
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import scala.util.Random

object ActorRecepcion:

  type RefJugador = ActorRef[ActorJugador.Mensaje]

  enum Mensaje:
    case CrearSala(nombre: String, jugador: RefJugador)
    case UnirseASala(codigo: String, nombre: String, jugador: RefJugador)
    case SalaCerrada(codigo: String)
  import Mensaje.*

  def apply(): Behavior[Mensaje] = activa(Map.empty)

  private def activa(salas: Map[String, ActorRef[ActorSala.Mensaje]]): Behavior[Mensaje] =
    Behaviors.receive { (contexto, mensaje) =>
      mensaje match
        case CrearSala(nombre, jugador) =>
          val codigo = codigoLibre(salas.keySet)
          val sala   = contexto.spawn(ActorSala(codigo), s"sala-$codigo")
          contexto.watchWith(sala, SalaCerrada(codigo))
          sala ! ActorSala.Mensaje.Unirse(nombre, jugador)
          activa(salas + (codigo -> sala))

        case UnirseASala(codigo, nombre, jugador) =>
          salas.get(codigo) match
            case Some(sala) => sala ! ActorSala.Mensaje.Unirse(nombre, jugador)
            case None       => jugador ! ActorJugador.Mensaje.Enviar(Json.error("No existe una sala con ese código."))
          Behaviors.same

        case SalaCerrada(codigo) =>
          activa(salas - codigo)
    }

  private val letras      = "BCDFGHJKLMNPQRSTVWXZ"
  private val largoCodigo = 4

  private def codigoLibre(usados: Set[String]): String =
    val codigo = Vector.fill(largoCodigo)(letras(Random.nextInt(letras.length))).mkString
    if usados.contains(codigo) then codigoLibre(usados) else codigo
