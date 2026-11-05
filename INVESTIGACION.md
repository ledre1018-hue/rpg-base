# Investigacion y explicaciones

Respuestas a los puntos del README que piden explicar algo. Las pruebas se ejecutan con:

```
java -cp out com.rpg.concurrency.LaboratorioConcurrencia
java -cp out com.rpg.persistencia.LaboratorioArchivos
```

## Concurrencia

### Hilo de usuario vs hilo daemon

Un hilo de usuario mantiene viva la JVM: el programa no termina hasta que todos acaban. Un hilo daemon es de
servicio: cuando solo quedan daemons vivos, la JVM cierra sin esperarlos. Por eso el auto-guardado, el
temporizador de turno, los efectos DoT/HoT y el lector de consola son daemon. `setDaemon(true)` debe llamarse
antes de `start()`; despues lanza `IllegalThreadStateException` (lo demuestra `LaboratorioConcurrencia`).

### Por que `synchronized`

`recibirDanio` y `curar` leen la vida, calculan y escriben, y ademas `setVida` cambia `estado`. Son varias
variables que deben cambiar juntas, asi que se protege el bloque entero con el monitor del propio `Personaje`.
El mismo candado protege `estado` y el mapa `efectos`, de modo que los hilos de efecto, el combate y el
auto-guardado nunca ven un estado a medias. En el laboratorio, la version sin proteccion pierde actualizaciones
y la sincronizada no.

### Por que `AtomicInteger.addAndGet(-danio)` no basta

Solo hace atomica la resta. `setVida` ademas acota la vida a `0..vidaMaxima` y pasa `estado` a `MUERTO`: son
dos variables relacionadas y un atomico no puede actualizarlas juntas. `updateAndGet(v -> Math.max(0, v - d))`
resolveria el acotado, pero el cambio de `estado` seguiria fuera de la operacion atomica.

### `ReentrantLock`

`GuerreroConLock` reescribe `recibirDanio` con `lock()` y `unlock()` dentro de `try-finally`. Lo preferiria a
`synchronized` cuando necesito `tryLock()` con tiempo de espera, bloqueos interrumpibles, un candado justo
(`fair`) o varias `Condition`. El `unlock()` va siempre en el `finally`: si una excepcion saltara antes, el
candado quedaria tomado para siempre.

No se usa dentro de `Personaje` porque mezclarlo con `synchronized` romperia la exclusion: `recibirDanio`
tomaria el lock y `curar` el monitor, y podrian ejecutarse a la vez.

## Archivos

### Ruta relativa vs absoluta

`new File("partidas/partida.dat")` es relativa: se resuelve desde el directorio donde se lanzo la JVM.
`getAbsolutePath()` devuelve la ruta completa desde la raiz del disco, que no depende de donde se ejecute.

### `File.delete()` vs `Files.deleteIfExists(Path)`

`File.delete()` devuelve `false` sin explicar por que fallo (no existe, sin permisos, carpeta no vacia).
`Files.deleteIfExists` devuelve `false` solo si no existia y lanza `IOException` con la causa en cualquier otro
error. `Files.delete` lanza `NoSuchFileException` si el archivo no existe.

### `FileWriter` vs `Files.writeString`

`BufferedWriter` + `FileWriter` es un flujo que abres, escribes por partes (por ejemplo `newLine()` en un bucle,
o en modo append) y cierras tu. `Files.writeString` (NIO) escribe todo el texto en una sola llamada y gestiona
la apertura y el cierre. Uso el flujo para el log que crece combate a combate, y `Files.writeString` para volcar
un texto pequeno y completo de una vez.

### `serialVersionUID`

Es la version de la clase serializada. Al leer, Java compara el valor guardado con el de la clase actual; si no
coinciden lanza `InvalidClassException`. Declararlo evita que el compilador calcule uno distinto ante cualquier
cambio menor y deja decidir cuando una partida antigua deja de ser compatible.

### `transient`

`Personaje.notificador` (una lambda hacia el registro de combate) y `Personaje.hilosEfectos` (objetos `Thread`)
son `transient`: no tiene sentido guardarlos y ademas no son serializables. Al cargar vuelven como `null` (lo
comprueba `LaboratorioArchivos`).

### Seguridad de `Serializable`

`readObject` reconstruye cualquier clase que aparezca en el archivo y ejecuta su logica de deserializacion. Un
archivo manipulado puede encadenar clases del classpath ("gadgets") para ejecutar codigo, o declarar colecciones
enormes y anidadas que agotan memoria y CPU. Por eso no se debe deserializar nada que no venga de una fuente de
confianza. `RepositorioPartida` limita el riesgo con un `ObjectInputFilter` que solo admite clases de
`com.rpg`, `java.lang`, `java.util` y `java.time`. Alternativas mas seguras: JSON (Jackson, Gson), XML o un
formato de texto propio, que solo crean los tipos que el programa espera.
