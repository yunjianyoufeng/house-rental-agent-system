FROM python:3.11-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
WORKDIR /app
RUN useradd --create-home --uid 10001 agent
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY --chown=agent:agent app ./app
COPY --chown=agent:agent knowledge ./knowledge
COPY --chown=agent:agent evals ./evals
RUN mkdir -p data/rag logs \
    && chown -R agent:agent /app
USER agent
EXPOSE 8001
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
