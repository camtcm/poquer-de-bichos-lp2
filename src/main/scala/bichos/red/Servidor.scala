package bichos.red

import bichos.actores.{ActorJugador, ActorRecepcion}
import org.apache.pekko.actor.typed.{ActorSystem, Props}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.ws.{Message, TextMessage}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.typed.scaladsl.{ActorSink, ActorSource}
import java.util.UUID
import scala.concurrent.Future

object Servidor:

  def iniciar(sistema: ActorSystem[ActorRecepcion.Mensaje], puerto: Int, carpetaWeb: String): Future[Http.ServerBinding] =
    given ActorSystem[ActorRecepcion.Mensaje] = sistema
    Http().newServerAt("0.0.0.0", puerto).bind(rutas(sistema, carpetaWeb))

  def rutas(sistema: ActorSystem[ActorRecepcion.Mensaje], carpetaWeb: String): Route =
    concat(
      path("ws")(handleWebSocketMessages(flujoDeJugador(sistema))),
      path("salud")(complete("ok")),
      pathEndOrSingleSlash(getFromFile(s"$carpetaWeb/index.html")),
      getFromDirectory(carpetaWeb)
    )

  private def flujoDeJugador(sistema: ActorSystem[ActorRecepcion.Mensaje]): Flow[Message, Message, Any] =
    given ActorSystem[ActorRecepcion.Mensaje] = sistema

    val (salida, haciaNavegador) = ActorSource
      .actorRef[String](
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        bufferSize = 64,
        overflowStrategy = OverflowStrategy.dropHead
      )
      .preMaterialize()

    val jugador = sistema.systemActorOf(ActorJugador(sistema, salida), s"jugador-${UUID.randomUUID()}", Props.empty)

    val desdeNavegador = Flow[Message]
      .collect { case TextMessage.Strict(texto) => ActorJugador.Mensaje.TextoRecibido(texto) }
      .to(
        ActorSink.actorRef(
          jugador,
          onCompleteMessage = ActorJugador.Mensaje.ConexionCerrada,
          onFailureMessage = _ => ActorJugador.Mensaje.ConexionCerrada
        )
      )

    Flow.fromSinkAndSourceCoupled(desdeNavegador, haciaNavegador.map(TextMessage(_)))
