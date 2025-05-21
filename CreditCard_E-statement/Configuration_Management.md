# Configuration Management Strategy

This document defines the strategy for managing application and infrastructure configurations across all environments for the CreditCard E-statement system.

## 1. Introduction & Goals

*   **Purpose:** To establish a consistent, secure, and auditable approach for managing all system configurations.
*   **Goals:**
    *   Ensure consistency of configurations across different deployment environments (development, testing, staging, production).
    *   Securely manage sensitive configuration data (secrets).
    *   Allow for easy modification and updates to configurations.
    *   Provide auditability for configuration changes.
    *   Support environment-specific settings without altering application code.
*   **Scope:** This strategy applies to configurations for all microservices, databases, message queues, API Gateway, AFP resources, third-party service integrations (e.g., email/SMS/push gateways), and operational parameters.

## 2. Configuration Storage & Formats

*   **Primary Configuration Formats:**
    *   **YAML (`.yaml` or `.yml`): Recommended for structured configuration.**
        *   **Rationale:** Supports hierarchical data structures, comments, and is generally more human-readable for complex configurations than JSON or `.env` files.
        *   Example: `config.base.yaml`, `config.production.yaml`.
    *   **`.env` Files (for environment variable definitions):**
        *   **Rationale:** Simple key-value pairs, easily loaded into environment variables by various tools and languages. Useful for bootstrapping or overriding specific settings.
        *   Example: `.env.production` (values from this would typically be loaded into the environment by the deployment system).
    *   **JSON (`.json`):** Can be used, but YAML is often preferred for manual editing. JSON is suitable for machine-generated or consumed configurations.
*   **Environment Variables as Primary Consumption Method:**
    *   Applications (especially those containerized with Docker) SHOULD primarily consume configuration values via environment variables.
    *   This decouples configuration from the application artifact (e.g., Docker image).
    *   Configuration values from YAML files, `.env` files, Kubernetes ConfigMaps/Secrets, or dedicated configuration services can be mapped to environment variables available to the application process.
*   **Version Control:**
    *   **Default/Base Configurations:** Non-sensitive default configuration files or templates (e.g., `config.defaults.yaml`, `.env.example`) SHOULD be committed to the Git repository alongside the application code. These define the structure and provide sensible defaults.
    *   **Environment-Specific Configurations:**
        *   Files containing secrets (e.g., database passwords, API keys) MUST NOT be committed to Git. Use `.gitignore` to explicitly exclude these files (e.g., `*.local.yaml`, `.env.*.local`, `secrets.yaml`).
        *   Non-sensitive environment-specific overrides can be version-controlled if appropriate, but secrets must always be externalized.

## 3. Environment-Specific Configurations

*   **Defined Environments:** The system will have clearly defined environments, such as:
    *   `development` (local developer machines)
    *   `ci` (continuous integration server)
    *   `testing` (shared QA environment)
    *   `staging` (pre-production, UAT environment)
    *   `production` (live environment)
*   **Loading Strategy & Precedence:**
    1.  **Application Defaults:** Hardcoded defaults within the application (should be minimal).
    2.  **Base Configuration File:** A base configuration file (e.g., `config.base.yaml`) loaded by the application, containing common settings.
    3.  **Environment-Specific Configuration File:** An environment-specific file (e.g., `config.production.yaml`, `config.development.yaml`) that overrides values from the base file. The selection of this file can be determined by an environment variable (e.g., `APP_ENV=production`).
    4.  **Environment Variables:** Values set as environment variables in the execution environment (e.g., set by Docker, Kubernetes, or the OS) take the highest precedence and override values from any loaded files.
    5.  **Externalized Secrets:** Secrets fetched from a dedicated secrets management system override any file-based or environment variable settings for those specific secret keys.
*   **Tools for Environment Management:**
    *   **Docker Compose (for local development):** Use `env_file` or `environment` directives in `docker-compose.yml` to manage environment-specific settings for local containers.
    *   **Kubernetes (for testing, staging, production):**
        *   **ConfigMaps:** Store non-sensitive configuration data.
        *   **Secrets:** Store sensitive configuration data (see next section).
        *   These are mounted into pods as environment variables or files.
        *   Helm charts can be used to template Kubernetes manifests and manage environment-specific values.

## 4. Sensitive Configuration (Secrets Management)

*   **Definition:** Any configuration parameter that, if exposed, could lead to a security compromise. Examples: API keys, database passwords, private cryptographic keys (for DKIM, TLS), `client_secret` for OAuth, encryption keys for data at rest.
*   **Storage and Access (Prioritized Options):**
    1.  **Dedicated Secrets Management System (Highly Recommended):**
        *   **Tools:** HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, Google Cloud Secret Manager.
        *   **Process:** Applications authenticate to the secrets manager at runtime (e.g., via IAM roles in cloud environments, Kubernetes service account integration) and fetch their required secrets.
        *   **Benefits:** Centralized control, strong encryption at rest and in transit, fine-grained access policies, audit trails for secret access, secret rotation capabilities.
    2.  **Kubernetes Secrets (with etcd encryption enabled):**
        *   **Usage:** Store secrets as native Kubernetes Secret objects.
        *   **Security Note:** By default, Kubernetes Secrets are only base64 encoded, not encrypted within etcd. It is CRUCIAL to enable encryption at rest for etcd in the Kubernetes cluster if using this as the primary mechanism for sensitive secrets.
        *   **Integration:** Can be integrated with external secrets managers (e.g., External Secrets Operator for K8s) to sync secrets from systems like Vault into native K8s Secrets, providing a hybrid approach.
    *   **Environment Variables (from secure injection):** Secrets can be injected as environment variables into containers by the orchestration platform (Kubernetes) from a secure source (like K8s Secrets or a secrets management tool). Application code should read them from the environment.
*   **Access Control:** Access to secrets (both in the management system and how they are exposed to applications) MUST be strictly controlled using the principle of least privilege.
*   **Rotation:** Implement policies and automated procedures for regularly rotating secrets (e.g., database passwords, API keys). Secrets management systems often provide features to facilitate this.

## 5. Configuration for Different Components

*   **Microservices (General):** Database connection strings, message queue broker addresses, API keys for third-party services (e.g., SMS/Push gateways), logging levels, retry policy parameters (max attempts, backoff intervals), feature flag settings, resource paths (if not discovered via service discovery).
*   **AFP Resources:** Paths to AFP font libraries, overlay directories, page segment directories. These will likely be configured as volume mounts in containers, with the mount paths potentially specified via environment variables derived from ConfigMaps.
*   **SMTP Settings (Email Sender Service):** SMTP server address, port, authentication credentials (username/password if using a relay), `MAIL FROM` address, DKIM selector and private key (the key itself is a secret).
*   **Conversion Tool Parameters (AFP Conversion Module):** Path to the external conversion tool executable or SDK configuration, license key/server information (if applicable), default conversion parameters (e.g., PDF/A compliance level).
*   **API Gateway:** Routing rules, rate limiting policies, timeout settings, security policy configurations (e.g., JWT validation parameters, OAuth provider details).
*   **Batch Orchestrator (Apache Airflow):** Database connection for its metadata store, executor configuration, connections to external services (e.g., Kubernetes cluster, cloud services), variables and macros for DAGs.

## 6. Configuration Management Tools & Practices

*   **Kubernetes ConfigMaps and Secrets:** As mentioned, these are the native way to manage application configuration in Kubernetes.
*   **Helm (for Kubernetes deployments):**
    *   Helm charts allow templating of Kubernetes manifests.
    *   Environment-specific configurations can be managed using different `values.yaml` files per environment or by overriding values during Helm deployment.
*   **Ansible, Chef, Puppet (less relevant for fully K8s-native apps):** If managing VMs or traditional server deployments, these tools can be used for configuration file management and distribution.
*   **Configuration Validation:**
    *   Applications SHOULD validate their configuration on startup.
    *   Fail fast if critical configurations are missing or invalid (e.g., unable to connect to database or message queue).
    *   Log any configuration issues clearly.
*   **Auditing Configuration Changes:**
    *   Changes to default configurations (in Git) are audited via Git history.
    *   Changes to live configurations (e.g., updating a ConfigMap in Kubernetes, modifying a secret in Vault) MUST be auditable. Secrets management systems and Kubernetes API server provide audit logs.
    *   Implement change management processes for production configuration changes.
*   **Dynamic Configuration / Feature Flags (Optional/Advanced):**
    *   **Concept:** For certain types of configuration (e.g., feature flags, tuning parameters, log levels), consider using a dynamic configuration system that allows changes to take effect without requiring an application restart.
    *   **Tools:** Spring Cloud Config Server, HashiCorp Consul, etcd, LaunchDarkly (for feature flags).
    *   **Considerations:** Adds complexity to the architecture. Use judiciously for parameters that genuinely need dynamic updates.

## 7. Review and Updates

*   Configuration parameters and the overall management strategy MUST be reviewed periodically (e.g., quarterly or as part of security reviews).
*   Remove obsolete or unused configuration parameters.
*   Update configurations as part of software upgrades (e.g., new parameters for a new version of a database or message queue).
*   Ensure documentation related to configuration is kept up-to-date.

This Configuration Management strategy aims to provide a secure, flexible, and auditable way to manage the diverse configuration needs of the CreditCard E-statement system.
