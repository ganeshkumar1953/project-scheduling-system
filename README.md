# Optimized Project Scheduling System

## Description

The **Optimized Project Scheduling System** is a robust, full-stack web application designed to eliminate scheduling conflicts and streamline project presentation bookings for institutions. This production-ready system connects students to available presentation slots while affording administrators complete authority over date management, slot creation, waitlists, and comprehensive status reporting. It ensures 100% data consistency via a Spring Boot backend, a MySQL database, and real-time vanilla JS DOM updates.

## Features

- **For Admins:**
  - Full CRUD control over Demo Dates and specific time slots.
  - See all incoming registrations, actively monitor waitlists, and override cancellations.
  - One-click CSV reporting and team generation details.

- **For Students:**
  - Automated team initialization flows and immediate email validations.
  - Interactive calendar interface to safely book, reschedule, or cancel a presentation slot without colliding with peers.
  
- **Intelligent Engine:**
  - Hardened concurrency loops that lock slots instantly.
  - Auto-promotion for Waitlist members if their desired spot vacates. 

## Tech Stack

*   **Frontend:** Vanilla JS (`fetch`), HTML5, CSS3, DOM manipulations.
*   **Backend:** Spring Boot (Java), Spring Web, Spring Data JPA.
*   **Database:** MySQL Server.
*   **Deployment Environment:** Railway.app (CI/CD Pipeline).

## 🚀 Live Demo URL

The most current main branch is continuously integrated and deployed live here:

**[https://project-scheduling-system-production.up.railway.app](https://project-scheduling-system-production.up.railway.app)**

## 🎮 How to Use (Access Instructions)

Simply open your web browser and click on the Live URL above! There is no local setup necessary unless you're a developer. 

### To use the system as an Administrator:
1. Click the **"Admin Login"** equivalent on the Live Portal. 
2. **Username**: *(Provide your configured admin username here, typically `admin` via system variables)*
3. **Password**: *(Provide your configured admin password here)*
4. Once logged in, navigate the portal to open up dates and generate slots!

### To use the system as a Student:
1. Navigate to the Live URL.
2. Under **Student Registration**, input your Team Name, emails and metrics to officially instantiate your team.
3. Once registered, view the open available slots listed.

## 🧪 Demo Flow

Try mimicking this standard operation flow:
1. **Register**: Sign up a test team with your email via the Student module.
2. **Login Admin & Create Slot**: (Simulate) An Administrator allocates a 2:00PM slot for tomorrow.
3. **Student Books Slot**: Go back as a student—the 2:00PM slot is immediately available. Click "Book Slot".
4. **Waitlist Trigger**: (Optional) Try registering a _second_ team and attempting to book that exact 2:00PM slot to see the waitlist behavior capture your queue perfectly!

## Basic API Endpoints

-   `POST /api/students/teams` – Initializes a new team registration.
-   `GET /api/schedule/slots` – Fetches live open presentation paths for teams.
-   `POST /api/schedule/slots/book` – Locks down an available slot.
-   `GET /api/admin/waiting-list` – Retrieves current waitlisted queue details.

## Deployment Info

This application guarantees stability via standard environment scaling processes.
*   **Railway.app** watches the `main` GitHub branch.
*   Any code merges instantly trigger a Maven `.jar` compilation container on Railway.
*   The connection dynamically maps directly to Railway's Managed MySQL module using secure ENVs (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), ensuring **no hardcoded credentials** live within this repository. 

## Author

- **[Ganesh Kumar M - ganeshkumar1953](https://github.com/ganeshkumar1953)** (Developer)