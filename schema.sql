CREATE DATABASE IF NOT EXISTS rpg_arena_db CHARACTER SET utf8mb4;
USE rpg_arena_db;

CREATE TABLE IF NOT EXISTS jugadores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    clave VARCHAR(100) NOT NULL,
    rol ENUM('jugador', 'admin') NOT NULL DEFAULT 'jugador'
);

CREATE TABLE IF NOT EXISTS monstruos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    vida INT NOT NULL CHECK (vida > 0),
    ataque INT NOT NULL CHECK (ataque > 0)
);

CREATE TABLE IF NOT EXISTS victorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    jugador_id INT NOT NULL,
    monstruo_id INT NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_victoria_jugador FOREIGN KEY (jugador_id) REFERENCES jugadores(id) ON DELETE CASCADE,
    CONSTRAINT fk_victoria_monstruo FOREIGN KEY (monstruo_id) REFERENCES monstruos(id) ON DELETE RESTRICT
);

INSERT IGNORE INTO jugadores (usuario, clave, rol) VALUES ('admin', 'admin123', 'admin'), ('jugador1', '1234', 'jugador');
INSERT IGNORE INTO monstruos (nombre, vida, ataque) VALUES ('Goblin', 50, 8), ('Arana Gigante', 60, 10), ('Ogro', 100, 14), ('Dragon Joven', 160, 20);
