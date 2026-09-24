# Gunicorn configuration for AVMS (config.wsgi.application).
# Run: gunicorn --config gunicorn.conf.py config.wsgi
bind = "0.0.0.0:8000"
workers = 3
timeout = 60
graceful_timeout = 30
accesslog = "-"
errorlog = "-"
loglevel = "info"