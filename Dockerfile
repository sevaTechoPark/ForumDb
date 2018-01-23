FROM ubuntu:16.04

RUN apt-get -y update

ENV PGVER 9.5
RUN apt-get install -y postgresql-$PGVER

USER postgres

RUN /etc/init.d/postgresql start &&\
    psql --command "CREATE USER docker WITH SUPERUSER PASSWORD 'docker';" &&\
    createdb -E UTF8 -T template0 -O docker docker &&\
    /etc/init.d/postgresql stop

RUN echo "host all  all    0.0.0.0/0  md5" >> /etc/postgresql/$PGVER/main/pg_hba.conf

RUN echo "listen_addresses='*'" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "synchronous_commit=off" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "shared_buffers = 128MB" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "max_wal_size = 1GB" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "work_mem = 4MB" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "fsync = off" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "full_page_writes = false" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "maintenance_work_mem = 256MB" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "effective_cache_size = 256MB" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "unix_socket_directories = '/var/run/postgresql/'" >> /etc/postgresql/$PGVER/main/postgresql.conf \
&& echo "logging_collector = off" >> /etc/postgresql/$PGVER/main/postgresql.conf

EXPOSE 5432

VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"]

USER root

RUN apt-get install -y openjdk-8-jdk-headless
RUN apt-get install -y maven

ENV WORK /opt/Forum
ADD Forum/ $WORK/
WORKDIR $WORK

RUN mvn package

EXPOSE 5000

CMD service postgresql start && java -Xmx300M -Xss256k -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:+UseSerialGC -XX:VMThreadStackSize=256 -XX:InitialCodeCacheSize=4096 -XX:InitialBootClassLoaderMetaspaceSize=4096 -jar target/Forum-1.0-SNAPSHOT.jar
