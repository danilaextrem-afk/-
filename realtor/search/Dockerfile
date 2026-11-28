FROM python:3.12-slim
WORKDIR /srv
COPY requirements.txt requirements-browser.txt ./
RUN pip install --no-cache-dir -r requirements-browser.txt && playwright install --with-deps chromium
COPY service.py ./
ENV REALTOR_DB=/data/realtor-search.sqlite3
RUN mkdir /data
VOLUME ["/data"]
CMD ["uvicorn", "service:app", "--host", "0.0.0.0", "--port", "8080"]
