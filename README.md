# Spring Boot Authentication Template

A production-ready Spring Boot backend template with comprehensive authentication features including JWT, email verification, MFA, and user management.

## Features

- **Authentication & Authorization**
  - JWT-based authentication with refresh tokens
  - Email verification for new accounts
  - Password reset functionality
  - Multi-factor authentication (MFA) support
  - Role-based access control (USER, ADMIN)
  - Subscription tier management (FREE, PRO, PREMIUM)

- **Security**
  - Spring Security integration
  - Password encryption with BCrypt
  - JWT token validation
  - Account locking and email verification requirements
  - Configurable security policies

- **Database & Persistence**
  - JPA/Hibernate with PostgreSQL support
  - H2 database for development/testing
  - Flyway database migrations
  - Connection pooling with HikariCP

- **Email Integration**
  - SMTP email service configuration
  - Thymeleaf email templates
  - Verification and password reset emails
  - Mailpit for local development

- **API Documentation**
  - OpenAPI/Swagger UI documentation
  - RESTful API design
  - Comprehensive endpoint documentation

- **Development Tools**
  - Docker and Docker Compose setup
  - Adminer database administration
  - Hot reload for development
  - Code quality tools (Checkstyle, PMD, SpotBugs)
  - Test coverage with JaCoCo
  - OWASP dependency vulnerability scanning

## Quick Start

### Prerequisites

- Java 25+
- Maven 3.6+
- Docker and Docker Compose (for full setup)

### Local Development Setup

1. **Clone and configure:**
   ```bash
   git clone <repository-url>
   cd tco-backend-new
   ```

2. **Environment variables:**
   Create a `.env` file in the project root:
   ```env
   # Database
   DB_NAME=templatedb
   DB_USER=postgres
   DB_PASSWORD=postgres
   DB_PORT=5432

   # JWT
   JWT_SECRET=your-256-bit-secret-here
   JWT_EXPIRATION=86400000

   # Email (for production)
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   MAIL_SMTP_AUTH=true
   MAIL_SMTP_STARTTLS=true
   APP_EMAIL_FROM=noreply@yourapp.com

   # Application
   APP_PORT=8080
   ADMINER_PORT=8081
   MAILPIT_SMTP_PORT=1025
   MAILPIT_WEB_PORT=8025
   ```

3. **Run with Docker:**
   ```bash
   docker-compose up -d
   ```

   This starts:
   - PostgreSQL database
   - Spring Boot application
   - Adminer (database admin UI at http://localhost:8081)
   - Mailpit (email testing UI at http://localhost:8025)

4. **Access the application:**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - H2 Console (dev only): http://localhost:8080/h2-console

### Manual Setup (without Docker)

1. **Database setup:**
   ```bash
   # Install PostgreSQL or use H2 for development
   createdb templatedb
   ```

2. **Configure application.yaml:**
   Update `src/main/resources/application.yaml` with your database and email settings.

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:testdb` | Database connection URL |
| `JWT_SECRET` | `only-for-local-testing-change-in-production` | JWT signing secret (256-bit) |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server hostname |
| `MAIL_USERNAME` | - | SMTP authentication username |
| `MAIL_PASSWORD` | - | SMTP authentication password |
| `APP_EMAIL_FROM` | `noreply@template.com` | Sender email address |

See `application.yaml` for all available configuration options.

## API Endpoints

### Authentication

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/auth/verify-email` - Verify email address
- `GET /api/auth/me` - Get current user info
- `POST /api/auth/forgot-password` - Request password reset
- `POST /api/auth/reset-password` - Reset password
- `POST /api/auth/verify-mfa` - Verify MFA code

### User Management

- `GET /api/users` - List users (admin only)
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user (admin only)

## Database Schema

The application uses Flyway migrations. Initial schema includes:

- `users` - User accounts with authentication data
- `verification_tokens` - Email verification tokens
- `password_reset_tokens` - Password reset tokens
- `mfa_secrets` - MFA configuration

## Development

### Code Quality

**CI/CD Quality Pipeline:**
- **Build & Compile** *(blocking)*: Compilation and validation
- **Unit Tests** *(blocking)*: 36 comprehensive unit tests
- **Code Quality** *(blocking)*: SpotBugs & PMD (catch real bugs)
- **Code Style** *(informational)*: Checkstyle (guidelines, not blocking)

**Local Quality Checks:**
```bash
# Full verification (includes all checks)
./mvnw verify

# Quick quality check (recommended before push)
./scripts/quality-check.sh

# Individual checks
./mvnw test               # Unit tests
./mvnw spotbugs:check     # Bug detection
./mvnw pmd:check          # Code quality
./mvnw checkstyle:check   # Code style (informational)
./mvnw dependency-check:check -Ddependency.check.skip=false  # Security vulnerabilities
```

**Quality Checks (Pre-push):**
Quality checks run automatically before `git push` to ensure code quality:

```bash
# Automatic: Runs before every push
git push  # Quality checks run automatically

# Manual quality check (anytime)
./scripts/quality-check.sh

# Bypass checks (urgent situations)
SKIP_QUALITY_CHECKS=1 git push

# Interactive urgent push
./scripts/push-urgent.sh

# Or set environment variable
export SKIP_QUALITY_CHECKS=1
git push  # No checks will run
```

**Check Results:**
```bash
Running comprehensive quality checks...
========================================
1. Unit Tests
Running Unit Tests... PASSED

2. Code Quality Checks
Running SpotBugs (Bug Detection)... PASSED
Running PMD (Code Quality)... PASSED

3. Build Check
Running Clean Build... PASSED

========================================
All quality checks passed!

Safe to push your changes.
```

**Quality Gates:**
- ✅ **Unit Tests** (must pass - catches logic errors)
- ✅ **SpotBugs** (must pass - catches real bugs)
- ✅ **PMD** (must pass - catches code quality issues)
- ℹ️ **Checkstyle** (informational - style guidelines)
- ❌ **Dependency Security** (disabled in CI due to NVD API issues)

### Testing

The project includes comprehensive unit tests covering business logic, error handling, and edge cases.

**CI/CD Test Pipeline:**
- **Unit Tests Job**: Runs all 36 unit tests with JaCoCo coverage
- **Coverage Reports**: Generated and uploaded as artifacts
- **Test Results**: Surefire reports available for analysis

**Local Testing Commands:**
```bash
# Unit tests only
./mvnw test

# Unit tests with coverage report
./mvnw test jacoco:report

# Integration tests (future)
./mvnw verify -Dspring.profiles.active=test

# Run specific test class
./mvnw test -Dtest=AuthServiceTest

# Run specific test method
./mvnw test -Dtest=AuthServiceTest#login_ShouldReturnAuthResponse_WhenCredentialsValidAndNoMFA
```

**Test Coverage:**
- **UserService**: CRUD operations, validation, error handling
- **AuthService**: Registration, login, MFA, password reset, email verification
- **User Entity**: Business logic methods, builder patterns
- **36 total unit tests** with comprehensive mocking and assertions

**Test Reports:**
- Test results: `target/surefire-reports/`
- Coverage reports: `target/site/jacoco/` (open `index.html`)
- CI artifacts: Available in GitHub Actions workflow runs

### Building

```bash
# Build JAR
./mvnw clean package

# Build Docker image
docker build -t template-backend .
```

## Deployment

### Docker Deployment

```bash
# Build and run
docker-compose -f docker-compose.yml up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down
```

### Production Considerations

1. **Security:**
   - Change default JWT secret
   - Configure proper database credentials
   - Set up SSL/TLS certificates
   - Enable production logging levels

2. **Database:**
   - Use connection pooling
   - Configure database backups
   - Set up database monitoring

3. **Email:**
   - Configure SMTP provider (Gmail, SendGrid, etc.)
   - Set up email templates
   - Configure SPF/DKIM records

4. **Monitoring:**
   - Set up application monitoring
   - Configure log aggregation
   - Set up health checks

## Project Structure

```
src/main/java/com/kamran/template/
├── common/                 # Shared utilities and base classes
├── config/                 # Application configuration
├── security/               # Authentication and security
│   ├── auth/              # Authentication controllers and services
│   ├── config/            # Security configuration
│   └── jwt/               # JWT utilities
├── user/                  # User management
└── TemplateApplication.java

src/main/resources/
├── db/migration/          # Flyway migrations
├── templates/email/       # Email templates
└── application.yaml       # Application configuration

scripts/                    # Development utility scripts
├── quality-check.sh       # Manual quality checks
├── push-urgent.sh         # Emergency push bypass
└── README.md             # Scripts documentation
```

## Future Development

See [`docs/Future Enhancements.md`](docs/Future%20Enhancements.md) for potential areas of improvement and extension ideas.

## Contributing

1. Follow the existing code style and conventions
2. Write tests for new features
3. Update documentation as needed
4. Ensure all quality checks pass

## Deployment

For detailed deployment instructions, configuration requirements, and platform comparisons, see [`docs/Deployment Guide.md`](docs/Deployment%20Guide.md).

For production readiness assessment and critical implementation guidance, see [`docs/Production Readiness Checklist.md`](docs/Production%20Readiness%20Checklist.md).

## License

MIT License - see LICENSE file for details.

## Author

Kamran Rouhani
