FROM python:2
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt
COPY udpserver.py udpserver.py
EXPOSE 3000/udp
CMD python udpserver.py
