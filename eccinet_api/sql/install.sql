CREATE DATABASE IF NOT EXISTS eccinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eccinet;

CREATE TABLE IF NOT EXISTS campuses (
 id INT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(100) NOT NULL, zone VARCHAR(100) NOT NULL, address VARCHAR(220) NOT NULL,
 latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS categories (
 id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(80) NOT NULL UNIQUE,
 subtitle VARCHAR(100) NOT NULL, emoji VARCHAR(12) NOT NULL, tint VARCHAR(10) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS stores (
 id INT PRIMARY KEY AUTO_INCREMENT,
 category_id INT NOT NULL, campus_id INT NOT NULL, name VARCHAR(100) NOT NULL,
 description VARCHAR(220) NOT NULL, emoji VARCHAR(12) NOT NULL,
 latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL,
 pilot_only TINYINT(1) NOT NULL DEFAULT 1,
 FOREIGN KEY (category_id) REFERENCES categories(id), FOREIGN KEY (campus_id) REFERENCES campuses(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS products (
 id INT PRIMARY KEY AUTO_INCREMENT, store_id INT NOT NULL, category_id INT NOT NULL,
 name VARCHAR(100) NOT NULL, description VARCHAR(250) NOT NULL, emoji VARCHAR(12) NOT NULL,
 price INT NOT NULL, stock INT NOT NULL DEFAULT 60, featured TINYINT NOT NULL DEFAULT 0,
 active TINYINT(1) NOT NULL DEFAULT 1,
 FOREIGN KEY (store_id) REFERENCES stores(id), FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS users (
 id INT PRIMARY KEY AUTO_INCREMENT, full_name VARCHAR(120) NOT NULL,
 student_code CHAR(6) NOT NULL UNIQUE, email VARCHAR(190) NOT NULL UNIQUE,
 phone VARCHAR(25) NOT NULL, password_hash VARCHAR(255) NOT NULL,
 campus_id INT NOT NULL DEFAULT 1, accepted_terms_at DATETIME NOT NULL,
 verified_at DATETIME NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (campus_id) REFERENCES campuses(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS auth_sessions (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id INT NOT NULL,
 token_hash CHAR(64) NOT NULL UNIQUE, expires_at DATETIME NOT NULL,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS otp_codes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id INT NOT NULL,
 purpose ENUM('verify','reset') NOT NULL, otp_hash CHAR(64) NOT NULL,
 expires_at DATETIME NOT NULL, attempts INT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX idx_otp (user_id,purpose)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS orders (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id INT NOT NULL, campus_id INT NOT NULL,
 status ENUM('confirmado','preparando','en_camino','entregado','cancelado') NOT NULL DEFAULT 'confirmado',
 payment_method ENUM('efectivo','nequi','daviplata','tarjeta') NOT NULL,
 payment_status ENUM('pendiente','pagado') NOT NULL DEFAULT 'pendiente',
 subtotal INT NOT NULL, delivery_fee INT NOT NULL, total INT NOT NULL,
 courier_latitude DECIMAL(10,7) NULL, courier_longitude DECIMAL(10,7) NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(id), FOREIGN KEY (campus_id) REFERENCES campuses(id),
 INDEX idx_orders_user (user_id,created_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS order_items (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, product_id INT NOT NULL,
 quantity INT NOT NULL, unit_price INT NOT NULL,
 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
 FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS reviews (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id INT NOT NULL, store_id INT NOT NULL,
 rating TINYINT NOT NULL, comment VARCHAR(450) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY unique_review (user_id,store_id),
 FOREIGN KEY (user_id) REFERENCES users(id), FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS chat_messages (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL,
 sender ENUM('estudiante','domiciliario') NOT NULL, body VARCHAR(500) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS notifications (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id INT NOT NULL, order_id BIGINT NULL,
 title VARCHAR(140) NOT NULL, body VARCHAR(350) NOT NULL, is_read TINYINT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS schedule_template (
 id INT PRIMARY KEY AUTO_INCREMENT, weekday TINYINT NOT NULL,
 subject VARCHAR(100) NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL
) ENGINE=InnoDB;

INSERT INTO campuses (id,name,zone,address,latitude,longitude) VALUES
(1,'Sede Principal','Teusaquillo','Carrera 19 # 49-20, Bogotá',4.6391000,-74.0687000),
(2,'Sede Sur','Bosa','Calle 57 Z Sur # 75D-10, Bogotá',4.5983000,-74.1826000),
(3,'Sede Crisanto Luque','Santa Fe','Carrera 10 # 19-62, Bogotá',4.6077000,-74.0711000),
(4,'Sede Campus 170','Suba','Calle 170 # 49B-96, Bogotá',4.7525000,-74.0516000)
ON DUPLICATE KEY UPDATE name=VALUES(name),zone=VALUES(zone),address=VALUES(address);
-- Coordenadas ilustrativas, NO están verificadas como posición exacta de entradas.

INSERT INTO categories(id,name,subtitle,emoji,tint) VALUES
(1,'Comida','Platos · Bebidas','🍔','#FFF2D2'),
(2,'Servicios','Envíos · Encargos','📦','#E6F0FF'),
(3,'Materiales','Útiles · Impresiones','📚','#F3E4FA'),
(4,'Papelería','Cuadernos · Copias','✏️','#E7F5F0'),
(5,'Librería','Libros · Guías','📖','#FFF1E7'),
(6,'Electrónica','Cables · Accesorios','🔌','#E5F5FF'),
(7,'Salud e higiene','Cuidado cotidiano','🧴','#FFEBF0')
ON DUPLICATE KEY UPDATE subtitle=VALUES(subtitle),emoji=VALUES(emoji),tint=VALUES(tint);

INSERT INTO stores(id,category_id,campus_id,name,description,emoji,latitude,longitude) VALUES
(1,1,1,'Cafetería Demo ECCI','Comidas de muestra para el piloto. No representa un comercio afiliado.','🍽️',4.6427000,-74.0737000),
(2,1,2,'Sushi Campus Demo','Catálogo de demostración sin afiliación comercial.','🍣',4.6016000,-74.1846000),
(3,3,1,'Materiales Express Demo','Materiales y útiles para actividades académicas.','📚',4.6430000,-74.0710000),
(4,4,3,'Papelería Académica Demo','Copias, cuadernos y suministros.','📝',4.6117000,-74.0741000),
(5,5,4,'Librería Universitaria Demo','Libros, guías y textos de referencia.','📖',4.7541000,-74.0539000),
(6,6,1,'Tech ECCI Demo','Accesorios básicos de tecnología.','🔌',4.6426000,-74.0715000),
(7,7,2,'Bienestar Demo','Artículos de higiene general.','🧼',4.6012000,-74.1840000),
(8,2,3,'Encargos Campus Demo','Servicios de mensajería de prueba.','📦',4.6097000,-74.0737000)
ON DUPLICATE KEY UPDATE name=VALUES(name),description=VALUES(description),emoji=VALUES(emoji);

INSERT INTO products(id,store_id,category_id,name,description,emoji,price,stock,featured) VALUES
(1,1,1,'Sopa especial','Sopa del día con acompañamiento.','🍲',14500,50,1),
(2,2,1,'Sushi de vegetales','Roll de vegetales para el almuerzo.','🍣',19500,45,1),
(3,1,1,'Hamburguesa clásica','Hamburguesa con papas.','🍔',16500,60,1),
(4,1,1,'Jugo natural','Bebida fría de fruta.','🥤',6500,80,0),
(5,1,1,'Almuerzo universitario','Plato del día de prueba.','🍛',15000,70,1),
(6,3,3,'Kit de dibujo técnico','Regla, escuadras y lápiz.','📐',21000,50,0),
(7,3,3,'Memoria USB 32 GB','Unidad de almacenamiento portátil.','💾',24000,35,0),
(8,4,4,'Cuaderno universitario','Cuaderno de 100 hojas.','📒',8500,60,0),
(9,4,4,'Impresión B/N (10 hojas)','Servicio de impresión tamaño carta.','🖨️',3000,95,0),
(10,4,4,'Set de bolígrafos','Tres bolígrafos surtidos.','🖊️',6000,85,0),
(11,5,5,'Libro de algoritmos','Guía académica introductoria.','📘',32000,20,0),
(12,5,5,'Manual de redes','Texto de consulta para redes.','📗',29000,30,0),
(13,6,6,'Cable USB-C','Cable de carga y transferencia.','🔌',14500,55,0),
(14,6,6,'Adaptador HDMI','Adaptador básico para pantalla.','🖥️',27000,38,0),
(15,6,6,'Mouse USB','Ratón óptico para computador.','🖱️',19500,40,0),
(16,7,7,'Gel antibacterial','Presentación personal pequeña.','🧴',7000,50,0),
(17,7,7,'Pañuelos de papel','Paquete individual.','🧻',3500,90,0),
(18,8,2,'Entrega de documentos','Encargo de papeles entre sedes (demo).','📦',9000,35,0),
(19,8,2,'Recoger impresión','Encargo de recoger impresión (demo).','🚲',8000,35,0),
(20,3,3,'Cartulina y marcadores','Kit de presentación académica.','🖍️',12500,50,0)
ON DUPLICATE KEY UPDATE name=VALUES(name),description=VALUES(description),emoji=VALUES(emoji);

INSERT INTO schedule_template(id,weekday,subject,start_time,end_time) VALUES
(1,1,'Redes de Computadores','07:00:00','09:00:00'),
(2,1,'I+D+I 2','09:00:00','11:00:00'),
(3,2,'Gestión de las TIC','07:00:00','09:00:00'),
(4,2,'Estadística y Probabilidad','09:00:00','11:00:00'),
(5,3,'Desarrollo de Aplicaciones Móviles','07:00:00','09:00:00'),
(6,3,'Electiva 1 (Python)','09:00:00','11:00:00'),
(7,4,'Desarrollo de Aplicaciones Móviles','07:00:00','09:00:00'),
(8,4,'Estadística y Probabilidad','09:00:00','11:00:00'),
(9,4,'Gestión de las TIC','11:00:00','13:00:00'),
(10,5,'Redes de Computadores','07:00:00','09:00:00'),
(11,5,'Electiva 1 (Python)','09:00:00','11:00:00')
ON DUPLICATE KEY UPDATE subject=VALUES(subject),start_time=VALUES(start_time),end_time=VALUES(end_time);
