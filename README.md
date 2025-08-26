# Components

**Order App**

    Port: 8080
    Technology: Java Spring Boot with Maven
    Description: Main order management application that provides REST API endpoints
    Health Check: Available at /v3/api-docs endpoint

## **Test Scenarios**

The project includes three automated test scenario clients:

#### Scenario 1 Client: Tests N+1 equal orders pattern

#### Scenario 2 Client: Tests N+1 equal orders with different price to test limit validation

#### Scenario 3 Client: Tests validating client that became inactive while new orders were created

Each scenario client:

    Runs on Java 21 with Spring Boot
    Waits for the order-app to be healthy before starting
    Executes automated tests and exits
    Uses Maven for dependency management

Prerequisites

    Docker
    Docker Compose

# Getting Started

## Build and Run

Execute the following commands from the project root directory (at the same level as docker-compose.yml):
    
    docker compose build
    docker compose up

Execution Flow

    Order App Startup: The main application starts first and exposes API on port 8080
    Health Check: System waits for order-app to be healthy (responds to /v3/api-docs)

Sequential Test Execution:

    Scenario 1 Client runs first and completes
    Scenario 2 Client starts after Scenario 1 completes successfully
    Scenario 3 Client starts after Scenario 2 completes successfully


Completion: 

All scenario clients exit after completing their tests