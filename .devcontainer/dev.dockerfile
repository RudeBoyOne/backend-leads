# Devcontainer Dockerfile
FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Install packages and Java 21 Temurin + Maven (will also rely on features)
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget gnupg curl ca-certificates maven \
    && rm -rf /var/lib/apt/lists/*

# Use features for Java & Maven (declared in devcontainer.json)

# Set workdir
WORKDIR /workspace

# Default shell
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

