FROM ubuntu:latest

RUN \
  apt-get install -y \
    software-properties-common \
    git

# Install Java.
RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

# Define working directory.
WORKDIR /data

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# Define default command.
CMD ["bash"]

ENV HELLO 12

# Install lein (to compile Clojure)
ENV LEIN_ROOT true

RUN wget -q -O /usr/bin/lein \
    https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
    && chmod +x /usr/bin/lein


# get and compile the code
RUN git clone https://github.com/vlobanov/population.git .
RUN lein deps
RUN lein uberjar

# # get the data (density and cities)
RUN \
  cd data/ && \
  wget https://s3.amazonaws.com/felixlotok/d.tar.gz && \
  tar -xvf d.tar.gz && cd ..


ENV DEBIAN_FRONTEND noninteractive

RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list
RUN apt-get update && apt-get install -y mysql-server-5.6
RUN  \
    /etc/init.d/mysql start && \
    echo "create database population" | mysql && \
    cat /data/sql_cities_init.sql | mysql -D population

RUN echo "/etc/init.d/mysql start && java -jar target/uberjar/population-0.1.0-SNAPSHOT-standalone.jar" > entry.sh && \
    chmod +x entry.sh

EXPOSE 8080
# ENTRYPOINT exec ./entry.sh