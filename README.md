# Póquer de Bichos — Avance II

Juego de cartas multijugador (4 a 6 jugadores) hecho en Scala con actores de Pekko. Se juega
desde el navegador.

## Requisitos

- Java 17 o superior (JDK)
- sbt 1.x

Funciona en Mac y Windows. En Windows, Java debe estar en el `PATH` (variable `JAVA_HOME`
configurada) y conviene usar una ruta sin espacios ni tildes para la carpeta del proyecto.

## Ejecutar

En una terminal, dentro de la carpeta del proyecto:

```bash
sbt run
```

La primera vez tarda unos minutos porque descarga Scala y las librerías. Cuando aparezca
`listo en http://localhost:8080`, abrir esa dirección en el navegador.

Para detenerlo: `Ctrl+C`.

## Jugar

1. Escribir un nombre y pulsar **Crear sala**.
2. Los demás escriben su nombre y el código de 4 letras y pulsan **Unirse**.
3. Con 4 a 6 jugadores, el anfitrión pulsa **Iniciar partida**.

Para probar en una sola computadora, abrir 4 pestañas: cada una es un jugador.
