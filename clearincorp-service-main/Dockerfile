# Use Ubuntu 22.04 (jammy) base to avoid Debian package mismatches
FROM eclipse-temurin:17-jre-jammy
WORKDIR /llc-api
# Pass the Spring profile as a build argument
ARG SPRING_PROFILES_ACTIVE=local
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

# Headless Chrome runtime deps (Jammy)
RUN set -eux; \
  apt-get update; \
  apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    wget \
    unzip \
    xdg-utils \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libglib2.0-0 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libu2f-udev \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxkbcommon0 \
    libxrandr2 \
  ; rm -rf /var/lib/apt/lists/*

# Chrome for Testing + matching chromedriver (Last-Known-Good)
RUN set -eux; \
  JSON_URL="https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions.json"; \
  LKG="$(curl -fsSL "$JSON_URL" | grep -oE '"version": *"[^"]+"' | head -n1 | cut -d'"' -f4)"; \
  curl -fsSL -o /tmp/chrome.zip "https://storage.googleapis.com/chrome-for-testing-public/${LKG}/linux64/chrome-linux64.zip"; \
  curl -fsSL -o /tmp/driver.zip "https://storage.googleapis.com/chrome-for-testing-public/${LKG}/linux64/chromedriver-linux64.zip"; \
  unzip -q /tmp/chrome.zip -d /opt && mv /opt/chrome-linux64 /opt/chrome; \
  unzip -q /tmp/driver.zip -d /opt && mv /opt/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver; \
  chmod +x /usr/local/bin/chromedriver; \
  /opt/chrome/chrome --version; \
  /usr/local/bin/chromedriver --version; \
  rm -rf /tmp/*.zip

# Make paths visible to your app
ENV CHROME_BIN=/opt/chrome/chrome
# chromedriver is on PATH at /usr/local/bin/chromedriver

# Copy the JAR built by CI (Fix A)
COPY target/*.jar /llc-api/app.jar

# (Optional) non-root
RUN useradd -ms /bin/bash appuser
USER appuser

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}","-jar","/llc-api/app.jar"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
CMD curl -fsS http://localhost:8080/actuator/health | grep '"status":"UP"' || exit 1