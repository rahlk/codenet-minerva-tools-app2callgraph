FROM gradle:6.7.0-jdk11

ARG USERNAME=dev
ARG USER_GROUP_NAME=workspace
ARG USER_UID=1000
ARG USER_GID=1000
ARG JAVA_VERSION=11.0.11.hs-adpt
ARG GRADLE_VERSION=6.7

ARG PKG="sudo unzip zip vim make git tree curl wget jq"

# Install packages and create the user with passwordless sudo privileges
RUN apt-get update \
    && apt-get install -y ${PKG} \
    && apt-get autoremove -y \
    && apt-get clean -y \
    && groupadd --gid ${USER_GID} ${USER_GROUP_NAME} \
    && useradd --uid ${USER_UID} --shell /bin/bash --gid ${USER_GID} -m ${USERNAME} \
    && usermod -aG sudo $USERNAME \
    && echo %${USER_GROUP_NAME} ALL=\(ALL\) NOPASSWD:ALL > /etc/sudoers.d/${USER_GROUP_NAME} \
    && chmod 0440 /etc/sudoers.d/${USER_GROUP_NAME}

# Enable color terminal for docker exec bash
ENV TERM=xterm-256color