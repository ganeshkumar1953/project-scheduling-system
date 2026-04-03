# Optimized Project Scheduling System

## Description

The Optimized Project Scheduling System is a full-stack web application designed to streamline the project presentation scheduling process. It securely connects students to available presentation slots while affording administrators complete authority over date management, slot creation, waitlists, and comprehensive status reporting. By employing a RESTful API architecture, it provides an intuitive, robust, and real-time mechanism to eliminate double-bookings and optimize institutional project presentations.

## Features

**Admin Module**
- **Schedule Management:** Complete CRUD operations for demo dates and specific presentation slots.
- **Reporting & Review:** View all registered teams, track corresponding bookings, and export detailed reports in CSV format.
- **Operational Oversight:** Access and manage the automated waiting list, process cancellations, and perform administrative overrides.

**Student Module**
- **Team Initialization:** Intuitive flow for student team registration and data validation.
- **Slot Booking:** Real-time visibility into available demo slots, allowing immediate booking or insertion into a waitlist.
- **Self-Service Actions:** Secure portal to view personal team status, process cancellations, and reschedule conflicting bookings seamlessly.

**Smart Systems**
- **Automated Waitlist Promotion:** Intelligently bumps waitlisted teams into newly vacated spots immediately upon a cancellation.
- **Concurrency & Validation:** Built-in safeguards (such as email verifications and database constraints) gracefully handle edge-cases to maintain state consistency and avoid over-booking loops.

## Tech Stack

- **Frontend:** HTML5, CSS3 (Vanilla), JavaScript
- **Backend:** Java, Spring Boot, Maven
- **Database:** MySQL
- **Architecture:** REST API, Model-View-Controller (MVC)

## Project Structure

```text
project-scheduling-system/
├── frontend/
│   ├── index.html
│   ├── index.css
│   └── index.js
├── scheduling-backend/
│   ├── src/
│   │   ├── main/java/com/scheduling/  # Core Java logic
│   │   └── main/resources/            # Application configs
│   └── pom.xml                        # Maven dependencies
├── schema.sql                         # Database schema definition
├── test_suite.ps1                     # Functional test scripts
└── .gitignore                         # Build and IDE exclusions
```

## Installation & Setup

Follow these steps to establish a local instance for development and testing.

### 1. Clone the Repository
```bash
git clone https://github.com/ganeshkumar1953/project-scheduling-system.git
cd project-scheduling-system
```

### 2. Database Setup
1. Ensure your local MySQL instance is running.
2. Initialize the database executing the provided schema:
```bash
mysql -u your_username -p < schema.sql
```

### 3. Backend Setup
1. Navigate into the backend directory:
```bash
cd scheduling-backend
```
2. Update the database credentials located in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/scheduling_db
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 4. Run the Backend (Spring Boot)
Build and run the Maven project:
```bash
mvn clean install
mvn spring-boot:run
```
*The API will expose itself natively on port `8080`.*

### 5. Run the Frontend
Because it leverages vanilla languages natively supported by the browser, you may simply serve the `frontend/` directory across a local web server to access it. For instance, using Python:
```bash
cd ../frontend
python -m http.server 3000
```
Open `http://localhost:3000` in your web browser. 

## API Overview

### Student Endpoints
- `POST /api/students/teams` - Register an academic team
- `GET /api/schedule/slots` - Fetch currently available project time slots
- `POST /api/schedule/slots/book` - Complete a single slot booking sequence
- `PUT /api/schedule/slots/reschedule` - Shift a pre-existing booking
- `DELETE /api/schedule/slots/cancel` - Cancel a pre-existing booking

### Admin Endpoints
- `GET /api/admin/dates` - Request all scheduled demo dates
- `POST /api/admin/slots` - Initialize new presentation capacities
- `GET /api/admin/waiting-list` - Pull current waitlist queues
- `GET /api/admin/reports/csv` - Generate real-time system export reports

## Demo Flow

1. **Administration Set-up**: An Admin logs into the system to initialize the term's "Demo Dates" and subsequently divides dates into "Booking Slots".
2. **Registration Phase**: Students access the client application, submit team details, and receive validation configurations.
3. **Queue / Booking Event**: Student Teams visually interact with available slots. If a slot is completely filled, they can elect to join the respective Waitlist. 
4. **Automated Promotion**: In the case an existing booked team reschedules or cancels via the portal, the primary waitlisted team is natively resolved via the backend to officially hold that spot.
5. **Report Extraction**: The Administration generates finalized CSV reporting for grading and analytics. 

## Deployment Guide

For production readiness:
1. **Frontend**: The `frontend/` directory should be deployed to a CDN overlay or standard web host platform (e.g. Vercel, Netlify, or Nginx).
2. **Backend**: Package the Spring Boot app into an optimal `.jar` via Maven (`mvn clean package`) and deploy across scalable application servers (e.g. AWS EC2, Heroku, or Azure App Service).
3. **Database**: Utilize a managed Relational Database Service (RDS) to preserve MySQL continuity. Include updated environment credentials spanning your instance. 

## Screenshots

> *Add application screenshots here to demonstrate intuitive UI / UX functionalities.*

![Admin Dashboard Placeholder](https://via.placeholder.com/800x400?text=Admin+Dashboard+Preview)
![Student Booking Process Placeholder](https://via.placeholder.com/800x400?text=Student+Booking+View)

## Future Improvements

- **Authentication System**: Implement JWT-based OAUTH security for role-based verifications.
- **Live Notifications**: Integrate WebSockets for real-time pushing alerts representing spot promotions.
- **Dynamic Pagination**: Optimize endpoints with pagination structures for scaling enterprise institutions. 
- **Calendar Integration**: Allow synchronization with Google Calendar or Microsoft Outlook for participants.

## Author / Credits

- **Ganesh Kumar M** - *Lead Developer* - [GitHub Profile](https://github.com/ganeshkumar1953)
- Built leveraging standard MVC paradigms, optimizing traditional institutional constraints securely.