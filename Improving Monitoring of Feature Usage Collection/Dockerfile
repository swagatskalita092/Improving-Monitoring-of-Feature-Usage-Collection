# Run tests: docker build -t downloader-test . && docker run --rm downloader-test
# Author: Swagat Subhash Kalita
# Date: 2026-02-20

FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

RUN apk add --no-cache bash wget unzip && \
    wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip -O gradle.zip && \
    unzip -q gradle.zip && \
    mv gradle-8.5 gradle && \
    rm gradle.zip

ENV PATH="/app/gradle/bin:${PATH}"

COPY . .

CMD ["gradle", "test", "--no-daemon"]
