package bichos.red

import bichos.juego.*
import scala.util.Try

object Json:

  enum Comando:
    case Crear(nombre: String)
    case Unirse(codigo: String, nombre: String)
    case Iniciar
    case Jugar(accion: Accion)

  def leerComando(texto: String): Either[String, Comando] =
    Try(ujson.read(texto)).toOption.flatMap(comandoDesdeJson).toRight("Mensaje no válido.")

  private def comandoDesdeJson(json: ujson.Value): Option[Comando] =
    Try {
      json("tipo").str match
        case "crear"     => Some(Comando.Crear(json("nombre").str))
        case "unirse"    => Some(Comando.Unirse(json("codigo").str.trim.toUpperCase, json("nombre").str))
        case "iniciar"   => Some(Comando.Iniciar)
        case "responder" => Some(Comando.Jugar(Accion.Responder(json("esVerdad").bool)))
        case "ofrecer" =>
          for
            carta       <- bichoDesdeTexto(json("carta").str)
            declaracion <- bichoDesdeTexto(json("declaracion").str)
          yield Comando.Jugar(Accion.Ofrecer(carta, json("receptor").str, declaracion))
        case _ => None
    }.toOption.flatten

  case class Participante(id: String, nombre: String, conectado: Boolean)

  def error(mensaje: String): ujson.Value =
    ujson.Obj("tipo" -> "error", "mensaje" -> mensaje)

  def estado(
      codigo: String,
      participantes: Vector[Participante],
      anfitrion: Option[String],
      partida: Option[Partida],
      para: String
  ): ujson.Value =
    ujson.Obj(
      "tipo"      -> "estado",
      "codigo"    -> codigo,
      "tuId"      -> para,
      "anfitrion" -> opcional(anfitrion.map(ujson.Str(_))),
      "miembros" -> participantes.map(p =>
        ujson.Obj("id" -> p.id, "nombre" -> p.nombre, "conectado" -> p.conectado)
      ),
      "partida" -> opcional(partida.map(vistaPartida(_, para)))
    )

  def vistaPartida(partida: Partida, para: String): ujson.Value =
    val oferta = partida.fase match
      case Fase.EsperandoRespuesta(o) => Some(o)
      case _                          => None
    val (fase, turno) = partida.fase match
      case Fase.EsperandoOferta(turno) => ("oferta", turno)
      case Fase.EsperandoRespuesta(o)  => ("respuesta", o.receptor)
      case Fase.Terminada(perdedor, _) => ("terminada", perdedor)

    ujson.Obj(
      "ronda" -> partida.ronda,
      "fase"  -> fase,
      "turno" -> turno,
      "jugadores" -> partida.jugadores.map(j =>
        ujson.Obj(
          "id"             -> j.id,
          "nombre"         -> j.nombre,
          "cartasEnMano"   -> j.mano.size,
          "penalizaciones" -> j.penalizaciones.map(texto)
        )
      ),
      "tuMano" -> partida.jugador(para).map(_.mano).getOrElse(Vector.empty).map(texto),
      "oferta" -> opcional(oferta.map(vistaOferta(_, para))),
      "ultima" -> opcional(partida.ultima.map(vistaResolucion)),
      "final"  -> opcional(vistaFinal(partida.fase))
    )

  private def vistaOferta(oferta: Oferta, para: String): ujson.Value =
    val cartaVisible = if oferta.oferente == para then Some(oferta.carta) else None
    ujson.Obj(
      "oferente"    -> oferta.oferente,
      "receptor"    -> oferta.receptor,
      "declaracion" -> texto(oferta.declaracion),
      "carta"       -> opcional(cartaVisible.map(c => ujson.Str(texto(c))))
    )

  private def vistaResolucion(r: Resolucion): ujson.Value =
    ujson.Obj(
      "carta"       -> texto(r.carta),
      "declaracion" -> texto(r.declaracion),
      "oferente"    -> r.oferente,
      "receptor"    -> r.receptor,
      "dijoVerdad"  -> r.dijoVerdad,
      "acerto"      -> r.acerto,
      "penalizado"  -> r.penalizado
    )

  private def vistaFinal(fase: Fase): Option[ujson.Value] = fase match
    case Fase.Terminada(perdedor, Motivo.CuatroIguales(bicho)) =>
      Some(ujson.Obj("perdedor" -> perdedor, "motivo" -> "cuatroIguales", "bicho" -> texto(bicho)))
    case Fase.Terminada(perdedor, Motivo.SinCartas) =>
      Some(ujson.Obj("perdedor" -> perdedor, "motivo" -> "sinCartas"))
    case _ => None

  def texto(bicho: Bicho): String = bicho.toString.toLowerCase

  def bichoDesdeTexto(texto: String): Option[Bicho] =
    Bicho.values.find(bicho => this.texto(bicho) == texto)

  private def opcional(valor: Option[ujson.Value]): ujson.Value = valor.getOrElse(ujson.Null)
