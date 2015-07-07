FROM debian:jessie

# Expose the port 9000 to the host
EXPOSE 9000

# Packaged dependencies
RUN apt-get update && apt-get install -y ca-certificates git-core ssh python openjdk-7-jdk --no-install-recommends

# Fixes empty home
ENV HOME /root

# Set directory
ENV APP_PATH /opt/restcommander

RUN mkdir -p /root/.ssh
RUN ssh-keygen -q -t rsa -N '' -f /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa

# Avoid first connection host confirmation
RUN ssh-keyscan github.com > /root/.ssh/known_hosts

RUN echo "Host github.com\n\tStrictHostKeyChecking no\n" >> /root/.ssh/config

# Clone Rest Commander
RUN git clone https://github.com/eBay/restcommander.git  ${APP_PATH}

WORKDIR ${APP_PATH}

RUN chmod +x start_application_linux.sh
RUN chmod +x play-1.2.4/play

CMD sh start_application_linux.sh start && tail -F AgentMaster/logs/system.out
