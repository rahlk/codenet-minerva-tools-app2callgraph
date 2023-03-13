# A base image that uses Ubuntu 22.04 operating system
FROM ubuntu:22.04

# Custom arguments to specify versions of Java and Gradle to be installed in the docker container
ARG JAVA_VERSION=11.0.11.hs-adpt
ARG GRADLE_VERSION=6.7

# List of packages that need to be installed in the docker container initially
ARG PKG="sudo unzip zip vim make git tree curl wget jq"

# Set default shell used by RUN instructions of docker image 
SHELL ["/bin/bash", "-c"]

# Upgrade & update the apt package list and install required packages
RUN apt-get update \
    && apt-get install -y ${PKG} \
    && apt-get autoremove -y \
    && apt-get clean -y 

# Set environment variable for colors in docker exec bash commands
ENV TERM=xterm-256color

# Copying contents of current directory to /root/app2callgraph in the docker container
ADD . /root/app2callgraph

# Set SHELL environment variable
ENV SHELL /bin/bash

# Install sdkman and then use it to install installed java version and gradle
RUN curl -s "https://get.sdkman.io" | bash 
RUN source $HOME/.sdkman/bin/sdkman-init.sh && sdk install java ${JAVA_VERSION} && sdk install gradle ${GRADLE_VERSION}

# Setting the working directory to /root/app2callgraph in the docker container
WORKDIR /root/app2callgraph
