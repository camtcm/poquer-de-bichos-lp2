package bichos

import bichos.actores.ActorRecepcion
import bichos.red.Servidor
import org.apache.pekko.actor.typed.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Main:
  def main(args: Array[String]): Unit =
    val puerto     = sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(8080)
    val carpetaWeb = sys.env.getOrElse("CARPETA_WEB", "web")

    val sistema = ActorSystem(ActorRecepcion(), "poquer-de-bichos")

    Servidor.iniciar(sistema, puerto, carpetaWeb).onComplete {
      case Success(_) =>
        println(s"Póquer de Bichos (avance 2) listo en http://localhost:$puerto")
      case Failure(error) =>
        println(s"No se pudo iniciar el servidor: ${error.getMessage}")
        sistema.terminate()
    }(sistema.executionContext)

    Await.result(sistema.whenTerminated, Duration.Inf)
