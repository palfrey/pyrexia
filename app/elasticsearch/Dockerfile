FROM elasticsearch:1

RUN plugin -i mobz/elasticsearch-head
COPY config/ /usr/share/elasticsearch/config/
RUN chown -R elasticsearch:elasticsearch /usr/share/elasticsearch/
