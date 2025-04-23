# Batch Script Execution Monitor

This Spring Boot application allows users to execute batch scripts and monitor their execution in real-time using WebSockets. The application provides a responsive web interface built with Angular and features comprehensive test coverage.

## Key Features

### Real-time Script Execution Monitoring

- Direct console output capture from script execution
- Separate threads for reading standard output and error streams
- Real-time progress parsing from console output
- WebSocket streaming of console output and progress updates
- Support for multiple concurrent script executions

### Backend Capabilities

- Script execution management with process isolation
- Complete error handling for script execution failures
- Database storage of execution history and results
- REST API for script execution and monitoring
- WebSocket endpoints for real-time updates

### Frontend Interface

- Modern, responsive UI built with Angular and Angular Material
- Real-time console output display with stdout/stderr differentiation
- Auto-scrolling console with blinking cursor effect for running processes
- Progress indicators and status updates
- Form for configuring and executing new batch scripts

## Technical Architecture

### Backend (Spring Boot)

- **Java 21** with modern language features
- **Spring Boot 3.x** framework with Spring MVC and WebSocket support
- **Spring Data JPA** for database access
- **H2 Database** for development/testing
- **Gradle** build system
- **YAML configuration** for application properties
- **JUnit 5** for comprehensive testing
- **Mockito** for mocking in tests
- **Process API** for script execution

### Frontend (Angular)

- **Angular 15+** with TypeScript
- **Angular Material** components
- **STOMP** over WebSockets for real-time communication
- **RxJS** for reactive programming
- **Responsive design** for various screen sizes

## Component Breakdown

### Core Backend Components

1. **Script Execution Service**
    - Manages script processes
    - Captures output streams
    - Handles process completion

2. **Console Output Service**
    - Processes and stores script output
    - Parses progress information from output

3. **WebSocket Service**
    - Broadcasts real-time updates to clients
    - Manages different message types (console output, progress updates, status changes)

4. **Batch Execution Service**
    - Coordinates overall execution flow
    - Maintains execution records

### REST API Endpoints

- `POST /api/executions` - Start a new script execution
- `GET /api/executions` - List all executions
- `GET /api/executions/{id}` - Get execution details
- `GET /api/executions/{id}/console` - Get console output

### WebSocket Topics

- `/topic/progress` - Script execution progress updates
- `/topic/console-output` - Real-time console output
- `/topic/status` - Execution status changes

### Angular Components

1. **Execution List Component**
    - Displays all script executions
    - Shows status and progress for each execution

2. **Execution Detail Component**
    - Shows detailed information about a specific execution
    - Includes tabs for console output and results

3. **Console Output Component**
    - Real-time display of script output
    - Differentiates between standard output and error messages

4. **New Execution Dialog Component**
    - Form for configuring and starting new script executions

## Getting Started

### Prerequisites

- Java 21 JDK
- Node.js and npm (for Angular frontend)
- Gradle

### Running the Application

1. Clone the repository
2. Navigate to the project root directory
3. Build the backend:
   ```bash
   ./gradlew build
   ```
4. Start the Spring Boot application:
   ```bash
   ./gradlew bootRun
   ```
5. In a separate terminal, build and serve the Angular frontend:
   ```bash
   cd batch-monitor-ui
   npm install
   ng serve
   ```
6. Access the application at `http://localhost:4200`

## Testing

The application includes comprehensive tests for all components:

- Unit tests for services and utilities
- Controller tests for REST endpoints
- WebSocket integration tests
- Process handling tests.

Run tests with:
```bash
./gradlew test
```

View test coverage report at `build/reports/jacoco/test/html/index.html`

## Future Enhancements

1. Role-based access control for script execution
2. Script template management
3. Scheduled script executions
4. Email notifications for script completion/failure
5. Detailed execution analytics and reporting
6. Support for distributed script execution
