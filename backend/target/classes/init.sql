CREATE DATABASE IF NOT EXISTS pet_mgmt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE pet_mgmt;

-- 4.1 表：sys_user
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(100),
  role ENUM('ADMIN','STAFF','OWNER') NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4.2 表：pet
CREATE TABLE IF NOT EXISTS pet (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  species VARCHAR(30) NOT NULL,
  breed VARCHAR(50),
  gender ENUM('M','F','U') DEFAULT 'U',
  birth_date DATE,
  weight DECIMAL(5,2),
  sterilized TINYINT DEFAULT 0,
  photo_url VARCHAR(255),
  remark TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_pet_owner_id(owner_id),
  INDEX idx_pet_species(species),
  CONSTRAINT fk_pet_owner FOREIGN KEY (owner_id) REFERENCES sys_user(id)
);

-- 4.3 表：medical_record
CREATE TABLE IF NOT EXISTS medical_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pet_id BIGINT NOT NULL,
  doctor_name VARCHAR(50),
  visit_date DATETIME NOT NULL,
  complaint TEXT,
  diagnosis TEXT,
  treatment TEXT,
  attachments JSON,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_mr_pet_date(pet_id, visit_date),
  CONSTRAINT fk_mr_pet FOREIGN KEY (pet_id) REFERENCES pet(id)
);

-- 4.4 表：vaccine_record
CREATE TABLE IF NOT EXISTS vaccine_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pet_id BIGINT NOT NULL,
  vaccine_name VARCHAR(100) NOT NULL,
  shot_date DATE NOT NULL,
  next_due_date DATE NOT NULL,
  remind_days_before INT DEFAULT NULL,
  remind_status ENUM('PENDING','SENT','DISABLED') NOT NULL DEFAULT 'PENDING',
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_vr_due(next_due_date),
  CONSTRAINT fk_vr_pet FOREIGN KEY (pet_id) REFERENCES pet(id)
);

-- 4.5 表：boarding_order
CREATE TABLE IF NOT EXISTS boarding_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL UNIQUE,
  pet_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  staff_id BIGINT DEFAULT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  room_type VARCHAR(30),
  price_total DECIMAL(10,2) DEFAULT 0,
  status ENUM('CREATED','CONFIRMED','BOARDING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'CREATED',
  remark TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_bo_owner(owner_id),
  INDEX idx_bo_pet(pet_id),
  INDEX idx_bo_status(status),
  CONSTRAINT fk_bo_pet FOREIGN KEY (pet_id) REFERENCES pet(id),
  CONSTRAINT fk_bo_owner FOREIGN KEY (owner_id) REFERENCES sys_user(id)
);

-- 4.6 表：sys_config
CREATE TABLE IF NOT EXISTS sys_config (
  config_key VARCHAR(50) PRIMARY KEY,
  config_value VARCHAR(200) NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO sys_config(config_key, config_value) VALUES
('VACCINE_REMIND_DAYS_DEFAULT','7'),
('REMIND_JOB_CRON','0 0 9 * * ?');

-- 4.7 表：notification
CREATE TABLE IF NOT EXISTS notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_noti_user(user_id)
);

-- 初始化管理员账号 (密码: admin123)。若未执行或密码无法验证，后端启动时会自动创建/重置该账号。
-- BCrypt hash for 'admin123': $2a$10$N.zmdr9k7uOCQb97.AnqJOZDpDXDXLzSHtZLdRxYO2O.w2OYaLV5K
INSERT IGNORE INTO sys_user (username, password_hash, role, status) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb97.AnqJOZDpDXDXLzSHtZLdRxYO2O.w2OYaLV5K', 'ADMIN', 1);
