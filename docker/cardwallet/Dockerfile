# Docker environment for building Java Card

FROM openjdk:8-jdk

# Environment variables
ENV JC_HOME=/opt/javacard-sdk
ENV PATH=$JC_HOME/bin:$PATH

# Install required tools
RUN apt-get update && \
    apt-get install -y unzip ant wget && \
    apt-get clean

# Copy and install Java Card SDK
COPY cardwallet/external/java_card_devkit_tools-bin-v25.0.zip /tmp/
RUN mkdir -p $JC_HOME && \
    unzip /tmp/java_card_devkit_tools-bin-v25.0.zip -d $JC_HOME && \
    rm /tmp/java_card_devkit_tools-bin-v25.0.zip

# Copy project source code
RUN mkdir /code
WORKDIR /code
COPY cardwallet/ /code/

# Copy build script
COPY cardwallet/scripts/build.sh /scripts/build.sh
RUN chmod +x /scripts/build.sh

