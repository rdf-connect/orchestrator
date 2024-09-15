FROM archlinux:latest

# Developer utilities
RUN pacman --noconfirm -Syu curl git

# JVM
RUN pacman --noconfirm -Syu jdk-openjdk
ENV JAVA_HOME /usr/lib/jvm/default

# Python
RUN pacman --noconfirm -Syu python python-pipx
RUN pipx install --global hatch
RUN pipx install --global uv

# Node.js
RUN pacman --noconfirm -Syu nodejs
RUN pacman --noconfirm -Syu npm

# gRPC code generation.
RUN pacman --noconfirm -Syu protobuf

