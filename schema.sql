-- ============================================================
-- Optimized Project Scheduling System — Database Schema
-- Database: scheduling_db (auto-created by Spring Boot)
-- Run this manually to reset or initialize the schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS scheduling_db;
USE scheduling_db;

-- Demo Dates
CREATE TABLE IF NOT EXISTS schedule_date (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  demo_date DATE NOT NULL UNIQUE
);

-- Teams
CREATE TABLE IF NOT EXISTS team (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_name VARCHAR(255) NOT NULL,
  members      INT          NOT NULL,
  leader_name  VARCHAR(255) NOT NULL,
  email        VARCHAR(255) NOT NULL UNIQUE,
  description  TEXT
);

-- Team Members (1 Team → Many Members, max 5)
CREATE TABLE IF NOT EXISTS team_member (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  team_id     BIGINT       NOT NULL,
  member_name VARCHAR(255) NOT NULL,
  CONSTRAINT fk_member_team FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE
);

-- Slots
CREATE TABLE IF NOT EXISTS slot (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  schedule_date_id BIGINT       NOT NULL,
  start_time       TIME         NOT NULL,
  end_time         TIME         NOT NULL,
  status           VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
  CONSTRAINT fk_slot_date FOREIGN KEY (schedule_date_id) REFERENCES schedule_date(id)
);

-- Bookings
CREATE TABLE IF NOT EXISTS booking (
  id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
  team_id    BIGINT      NOT NULL,
  slot_id    BIGINT      NOT NULL,
  status     VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
  booked_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_booking_team FOREIGN KEY (team_id) REFERENCES team(id),
  CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES slot(id)
);
