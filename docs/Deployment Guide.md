# Deployment Guide

This guide covers deploying the Spring Boot authentication template to production environments. It focuses on practical, maintainable deployment strategies suitable for development teams and small to medium-sized applications.

## Configuration Requirements

### Database Configuration

The application requires PostgreSQL 15+ for production use. The application uses Flyway for database migrations and Hibernate for ORM.

#### Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/dbname
SPRING_DATASOURCE_USERNAME=your_db_user
SPRING_DATASOURCE_PASSWORD=your_db_password
SPRING_DATASOURCE_DRIVER=org.postgresql.Driver
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
SPRING_FLYWAY_ENABLED=true
```

#### PostgreSQL Hosting Options

##### Free Tier Options for Hobby Projects

**Supabase**
- **Free Tier**: 500MB database, 50MB file storage, 2GB bandwidth
- **Features**: Built-in authentication, real-time subscriptions, RESTful API
- **Best for**: Modern full-stack applications, real-time features
- **Limitations**: Row limits, bandwidth caps

**Railway**
- **Free Tier**: 512MB RAM, 1GB disk, PostgreSQL included
- **Features**: Git integration, automatic deployments, managed PostgreSQL
- **Best for**: Quick prototyping, integrated deployments
- **Limitations**: Resource limits, single region

**Render**
- **Free Tier**: 750 hours/month, managed PostgreSQL
- **Features**: Managed database, automatic backups, connection pooling
- **Best for**: Web applications, API services
- **Limitations**: Sleeps after 15 minutes of inactivity

**Neon**
- **Free Tier**: 512MB storage, compute included
- **Features**: Serverless PostgreSQL, branching (database branching)
- **Best for**: Development workflows, database branching
- **Limitations**: Storage limits, no custom extensions

**ElephantSQL**
- **Free Tier**: 20MB database, shared hosting
- **Features**: Managed PostgreSQL, web-based admin panel
- **Best for**: Simple applications, quick setup
- **Limitations**: Very limited storage, shared resources

##### Paid Options for Production

**AWS RDS PostgreSQL**
- **Pricing**: Pay-per-use, starts at ~$0.02/hour
- **Features**: High availability, automated backups, multi-AZ deployment
- **Best for**: Enterprise applications, high availability requirements

**Google Cloud SQL**
- **Pricing**: Pay-per-use, competitive with AWS
- **Features**: Integrated with GCP ecosystem, automated maintenance
- **Best for**: GCP-based infrastructure, global scaling

**Azure Database for PostgreSQL**
- **Pricing**: Pay-per-use, flexible scaling
- **Features**: Integration with Azure services, geo-redundancy
- **Best for**: Microsoft ecosystem, hybrid cloud deployments

**DigitalOcean Managed Databases**
- **Pricing**: Fixed monthly pricing, starts at $12/month
- **Features**: Automated backups, monitoring, easy scaling
- **Best for**: Cost-conscious deployments, simple scaling

**Aiven for PostgreSQL**
- **Pricing**: Pay-per-hour, transparent pricing
- **Features**: Multi-cloud deployment, advanced security
- **Best for**: Multi-cloud strategies, enterprise features

#### Database Configuration Considerations

**Connection Pooling**
- Uses HikariCP (configured by default)
- Recommended pool size: 5-20 connections based on application load
- Connection timeout: 30 seconds
- Maximum lifetime: 30 minutes

**Security**
- Always use SSL connections (`?sslmode=require`)
- Rotate credentials regularly
- Use least-privilege principle for database users
- Enable database auditing for production

**Performance**
- Monitor slow queries with `log_min_duration_statement`
- Use connection pooling to avoid connection overhead
- Configure appropriate `work_mem` and `maintenance_work_mem`
- Regular vacuum and analyze operations

**Backup Strategy**
- Automated daily backups with point-in-time recovery
- Test backup restoration procedures regularly
- Store backups in multiple regions for disaster recovery
- Retention period based on compliance requirements (30-90 days typical)

**Migration Management**
- Flyway handles schema migrations automatically
- Never modify migration files after deployment
- Test migrations on staging environment first
- Version control migration scripts

#### Database Schema Overview

The application creates the following tables:
- `users`: User accounts and authentication data
- `verification_tokens`: Email verification and password reset tokens
- `mfa_codes`: Multi-factor authentication codes

All tables include audit fields (`created_at`, `updated_at`) via the `BaseEntity` class.

### Email Service Configuration
Configure SMTP provider for transactional emails:

```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=your_api_key
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
APP_EMAIL_FROM=noreply@yourdomain.com
```

**Supported Providers:**
- SendGrid: Reliable delivery, good developer experience
- Resend: Modern API, competitive pricing
- AWS SES: Cost-effective for high volume
- Gmail: Simple for development/testing

### Security Configuration
Critical security settings for production:

```bash
JWT_SECRET=your_256_bit_secret_key
JWT_EXPIRATION=86400000
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL_ROOT=WARN
```

**Key Security Measures:**
- Generate cryptographically secure JWT secrets
- Configure HTTPS termination
- Set appropriate session timeouts
- Enable security headers and CORS policies

### Application Configuration
Production-specific settings:

```bash
SERVER_PORT=8080
SPRINGDOC_ENABLED=false
H2_CONSOLE_ENABLED=false
APP_PORT=8080
```

## Deployment Methods

### Container-Based Deployment (Recommended)

Docker containers provide consistent, reproducible deployments:

```dockerfile
# Production Dockerfile (multi-stage build)
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Benefits:**
- Environment consistency across development and production
- Simplified dependency management
- Easy scaling and orchestration

### Cloud Platform Deployment

#### Railway
**Best for:** Rapid prototyping and small applications
**Setup:** Connect GitHub repository, automatic deployments
**Pricing:** Generous free tier, pay-per-resource usage
**Trade-offs:** Limited control, vendor lock-in

#### Render
**Best for:** Simple web applications and APIs
**Setup:** Git integration, managed PostgreSQL
**Pricing:** Free tier available, predictable scaling
**Trade-offs:** Limited customization, US-based infrastructure

#### Fly.io
**Best for:** Global applications requiring low latency
**Setup:** Dockerfile-based, global edge network
**Pricing:** Generous free tier, usage-based
**Trade-offs:** Complex for beginners, learning curve

#### DigitalOcean App Platform
**Best for:** Cost-conscious deployments
**Setup:** Git integration, managed databases
**Pricing:** Predictable monthly costs
**Trade-offs:** Fewer managed services than AWS/GCP

#### AWS/GCP/Azure
**Best for:** Enterprise applications requiring advanced features
**Setup:** Complex infrastructure as code
**Pricing:** Pay-for-what-you-use, complex cost optimization
**Trade-offs:** High complexity, significant operational overhead

### Traditional Server Deployment

For organizations preferring server management:

```bash
# System requirements
- Ubuntu 22.04+ or RHEL 9+
- OpenJDK 25+
- PostgreSQL 15+
- Nginx or Apache for reverse proxy
- SSL certificates (Let's Encrypt recommended)
```

**Benefits:** Full control, cost-effective for steady traffic
**Drawbacks:** Manual maintenance, security patching responsibility

## CI/CD Pipeline

### GitHub Actions Example

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - name: Run tests
        run: ./mvnw test

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        run: docker build -t myapp:${{ github.sha }} .

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Deploy to production
        run: echo "Deploy logic here"
```

### Quality Gates

The pipeline should enforce:
- Unit test execution and coverage thresholds
- Static analysis (SpotBugs, PMD)
- Security scanning (dependency checks)
- Integration test execution
- Performance regression testing

## Step-by-Step Deployment

### 1. Environment Preparation

```bash
# Clone repository
git clone <repository-url>
cd tco-backend-new

# Create production configuration
cp .env.example .env.prod
# Edit .env.prod with production values
```

### 2. Database Setup

```bash
# For managed PostgreSQL (Railway, Render, etc.)
# Create database instance through platform dashboard

# For self-hosted PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo -u postgres createuser --createdb --pwprompt myapp_user
sudo -u postgres createdb -O myapp_user myapp_prod
```

### 3. Email Service Configuration

1. Choose provider (SendGrid recommended for reliability)
2. Create account and generate API key
3. Configure domain authentication for better deliverability
4. Update environment variables

### 4. Security Configuration

```bash
# Generate secure JWT secret
openssl rand -hex 32

# Configure HTTPS certificates
# Use certbot for Let's Encrypt certificates
sudo apt install certbot
sudo certbot certonly --standalone -d yourdomain.com
```

### 5. Application Deployment

#### Docker Compose (Simple)

```yaml
version: '3.8'
services:
  app:
    image: myapp:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    env_file:
      - .env.prod
    ports:
      - "8080:8080"
    depends_on:
      - postgres

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: myapp_prod
      POSTGRES_USER: myapp_user
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

#### Cloud Platform (Railway Example)

1. Connect GitHub repository
2. Configure environment variables
3. Set up PostgreSQL database
4. Deploy application
5. Configure custom domain and SSL

### 6. Verification and Testing

```bash
# Health check
curl https://yourdomain.com/actuator/health

# API testing
curl https://yourdomain.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","firstName":"Test","lastName":"User"}'
```

## Platform Comparison

| Platform | Ease of Use | Cost | Scalability | Control | Best For |
|----------|-------------|------|-------------|---------|----------|
| Railway | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | Prototyping, small apps |
| Render | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | Web apps, APIs |
| Fly.io | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Global apps, low latency |
| DigitalOcean | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | Cost-conscious deployments |
| AWS/GCP | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Enterprise, complex requirements |

## Security Considerations

### Network Security
- Configure firewalls to restrict access
- Use VPC/security groups for cloud deployments
- Implement rate limiting at infrastructure level
- Enable DDoS protection (Cloudflare, AWS Shield)

### Application Security
- Keep dependencies updated
- Use environment-specific secrets management
- Implement proper logging and monitoring
- Regular security audits and penetration testing

### Data Protection
- Encrypt sensitive data at rest and in transit
- Implement proper backup strategies
- Configure data retention policies
- Ensure GDPR/CCPA compliance where applicable

## Monitoring and Maintenance

### Essential Monitoring
- Application metrics (response times, error rates)
- Database performance and connection pooling
- Email delivery rates and bounce tracking
- Security event monitoring and alerting

### Log Management
- Centralized logging with structured formats
- Log retention policies
- Alert configuration for critical events
- Regular log analysis for security threats

### Backup Strategy
- Automated database backups
- Configuration backups
- Application artifact storage
- Disaster recovery testing

### Update Process
- Regular dependency updates and security patches
- Blue-green deployment for zero-downtime updates
- Rollback procedures and testing
- Change management documentation

This deployment guide provides a foundation for production deployment. Choose the platform that best matches your team's expertise, scalability requirements, and operational preferences.