package bichos.juego

enum Bicho:
  case Arana, Cucaracha, Sapo, Mosca, Rata, Escorpion, Murcielago, Chinche

case class Jugador(
    id: String,
    nombre: String,
    mano: Vector[Bicho],
    penalizaciones: Vector[Bicho]
)

case class Oferta(
    carta: Bicho,
    oferente: String,
    receptor: String,
    declaracion: Bicho
)

case class Resolucion(
    carta: Bicho,
    declaracion: Bicho,
    oferente: String,
    receptor: String,
    dijoVerdad: Boolean,
    acerto: Boolean,
    penalizado: String
)

enum Motivo:
  case CuatroIguales(bicho: Bicho)
  case SinCartas

enum Fase:
  case EsperandoOferta(turno: String)
  case EsperandoRespuesta(oferta: Oferta)
  case Terminada(perdedor: String, motivo: Motivo)

case class Partida(
    jugadores: Vector[Jugador],
    fase: Fase,
    ultima: Option[Resolucion],
    ronda: Int
):
  def jugador(id: String): Option[Jugador] = jugadores.find(_.id == id)

  def terminada: Boolean = fase match
    case Fase.Terminada(_, _) => true
    case _                    => false

enum Accion:
  case Ofrecer(carta: Bicho, receptor: String, declaracion: Bicho)
  case Responder(esVerdad: Boolean)
