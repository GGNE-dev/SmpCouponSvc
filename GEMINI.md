# GEMINI.md - SmpCouponSvc

This document provides context and instructions for AI interactions within the `SmpCouponSvc` project.

## Project Overview

`SmpCouponSvc` is a Java-based microservice built using the Spring Boot framework. 
Based on its name and initial dependencies, it is intended to serve as a coupon management service.

### Core Technologies
- **Java 21**: The primary programming language.
- **Spring Boot 3.5.11**: The core application framework.
- **Gradle (Kotlin DSL)**: Used for build automation and dependency management.
- **Spring Data JPA**: For database interaction and ORM.
- **Spring Web**: To provide RESTful APIs.
- **Spring Validation**: For data validation.
- **Lombok**: To reduce boilerplate code (getters, setters, etc.).
- **JUnit 5**: The default testing framework.

### Architecture
The project follows the standard Spring Boot directory structure:
- `src/main/java`: Contains the application source code.
- `src/main/resources`: Contains configuration files like `application.properties`.
- `src/test/java`: Contains the automated tests.

## Building and Running

The project includes the Gradle Wrapper (`gradlew`), so there is no need to install Gradle locally.

- **Build the project:**
  ```powershell
  ./gradlew build
  ```
- **Run the application:**
  ```powershell
  ./gradlew bootRun
  ```
- **Run tests:**
  ```powershell
  ./gradlew test
  ```
- **Clean the build:**
  ```powershell
  ./gradlew clean
  ```

## Development Conventions
- **Style**: Follow standard Java and Spring Boot coding conventions. 
  - Use camelCase for methods and variables, PascalCase for classes.
- **Boilerplate**: Use Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Data`, `@Slf4j`) where appropriate to keep the code concise.
- **Validation**: Leverage Spring's `@Valid` and JSR-303/JSR-380 annotations for input validation.
- **Testing**: Ensure that all new features or bug fixes are accompanied by appropriate test cases in the `src/test/java` directory.
- **Configuration**: Use `src/main/resources/application.properties` for application-level configuration.
