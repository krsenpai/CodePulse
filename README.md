# CodePulse

CodePulse is a backend code execution and evaluation platform built with Java and Spring Boot.

It allows users to submit Java code, execute it inside an isolated Docker container, receive the execution result, and view their execution history.

## Features

- Java code compilation and execution
- Optional standard input
- Standard output and error handling
- Execution status tracking
- Execution time measurement
- PostgreSQL execution history
- User registration and login
- BCrypt password hashing
- JWT-based authentication
- User-specific execution history
- Paginated execution history
- Individual execution lookup
- Docker-based code isolation
- CPU and memory limits
- Process limits
- Network isolation
- Execution timeout
- Output size limit

## Tech Stack

- Java 25
- Spring Boot 4
- Spring Security
- JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Docker
- Maven

## Architecture

```text
Client
   |
   v
Controller
   |
   v
DTO
   |
   v
Service
   |
   +------> PostgreSQL
   |
   +------> Docker Execution Engine
                  |
                  v
             Java Container