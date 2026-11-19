package bichos.actores

import bichos.red.Json
import bichos.red.Json.Comando
import org.apache.pekko.actor.typed.{ActorRef, Behavior, Terminated}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object ActorJugador:

  enum Mensaje:
    case TextoRecibido(texto: String)
    case ConexionCerrada
    case Enviar(json: ujson.Value)
    case EntroASala(sala: ActorRef[ActorSala.Mensaje])
  import Mensaje.*

  def apply(recepcion: ActorRef[ActorRecepcion.Mensaje], salida: ActorRef[String]): Behavior[Mensaje] =
    activo(recepcion, salida, sala = None)

  private def activo(
      recepcion: ActorRef[ActorRecepcion.Mensaje],
      salida: ActorRef[String],
      sala: Option[ActorRef[ActorSala.Mensaje]]
  ): Behavior[Mensaje] =
    Behaviors
      .receive[Mensaje] { (contexto, mensaje) =>
        val yo = contexto.self
        mensaje match
          case TextoRecibido(texto) =>
            (Json.leerComando(texto), sala) match
              case (Left(motivo), _) =>
                salida ! ujson.write(Json.error(motivo))
              case (Right(Comando.Crear(nombre)), None) =>
                recepcion ! ActorRecepcion.Mensaje.CrearSala(nombre, yo)
              case (Right(Comando.Unirse(codigo, nombre)), None) =>
                recepcion ! ActorRecepcion.Mensaje.UnirseASala(codigo, nombre, yo)
              case (Right(_), None) =>
                salida ! ujson.write(Json.error("Primero entra a una sala."))
              case (Right(Comando.Iniciar), Some(ref)) =>
                ref ! ActorSala.Mensaje.Iniciar(yo)
              case (Right(Comando.Jugar(accion)), Some(ref)) =>
                ref ! ActorSala.Mensaje.Jugar(yo, accion)
              case (Right(_), Some(_)) =>
                salida ! ujson.write(Json.error("Ya estás en una sala."))
            Behaviors.same

          case Enviar(json) =>
            salida ! ujson.write(json)
            Behaviors.same

          case EntroASala(nuevaSala) =>
            contexto.watch(nuevaSala)
            activo(recepcion, salida, Some(nuevaSala))

          case ConexionCerrada =>
            sala.foreach(_ ! ActorSala.Mensaje.Desconectar(yo))
            Behaviors.stopped
      }
      .receiveSignal { case (_, Terminated(_)) =>
        salida ! ujson.write(Json.error("La sala se cerró."))
        activo(recepcion, salida, None)
      }
