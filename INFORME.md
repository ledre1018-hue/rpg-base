# Informe: Arena Segura (JDBC + MVC)

## 1. Diagrama del flujo View -> Controller -> DAO -> BD para el login

```
LoginUI --usuario, clave--> LoginController --autenticar()--> JugadorDAO --SELECT .. WHERE usuario=? AND clave=?--> MySQL
LoginUI <--mensaje--------- LoginController <--Jugador o null-- JugadorDAO <--ResultSet---------------------------- MySQL
```

El Controller valida campos y cuenta intentos (max 3); el DAO usa PreparedStatement y cierra el ResultSet.

## 2. Tabla comparativa Statement vs PreparedStatement

1000 INSERT en una tabla temporal (autocommit, 3 rondas):

| Criterio | Statement | PreparedStatement |
|---|---|---|
| Seguridad | `admin' OR 1=1 -- ` entra sin clave | La misma entrada falla |
| Ronda 1 | 1432 ms | 1359 ms |
| Ronda 2 | 1337 ms | 1324 ms |
| Ronda 3 | 1326 ms | 1308 ms |
| Repeticion de consultas | SQL nuevo en cada vuelta | Se prepara una vez, solo cambian los parametros |

La diferencia de tiempo es pequena (casi todo se va en confirmar cada INSERT en disco); la ventaja decisiva es la seguridad.

## 3. Cuando usar executeQuery y cuando executeUpdate

- `executeQuery()`: para `SELECT`; devuelve un `ResultSet`.
- `executeUpdate()`: para `INSERT`, `UPDATE`, `DELETE` (y DDL); devuelve un `int` con las filas afectadas.

## 4. Que error encontraste y como lo resolviste

- Sin la dependencia falla con `java.sql.SQLException: No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db`. Se resolvio agregando `mysql-connector-j` 8.4.0 al `pom.xml`.
- `admin' OR 1=1 --` sin espacio final daba error de sintaxis: MySQL solo toma `-- ` como comentario con el espacio. Se uso `admin' OR 1=1 -- `.

## 5. Evidencia de la inyeccion SQL

```
Usuario: [admin' OR 1=1 -- ]  Clave: [no_la_se]
--- Login inseguro (Statement + concatenacicat ~/commits_jdbc.shon) ---
SQL enviado: SELECT id FROM jugadores WHERE usuario = 'admin' OR 1=1 -- ' AND clave = 'no_la_se'
ACCESO CONCEDIDO sin conocer la clave
--- Login seguro (JugadorDAO.autenticar con PreparedStatement) ---
Acceso denegado
```

Con Statement la comilla cierra el texto, `OR 1=1` vuelve verdadera la condicion y `-- ` comenta la clave. Con PreparedStatement (el Escudo) la estructura del SQL queda fija antes de recibir los datos: cada `?` es solo un valor, nunca codigo, asi que `admin' OR 1=1 -- ` se busca como un nombre de usuario literal y el login falla.
