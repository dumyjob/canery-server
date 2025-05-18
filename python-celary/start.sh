# 启动Web服务
gunicorn app:app -w 4 -b 0.0.0.0:5000 &
# 启动Celery Worker
celery -A tasks worker --loglevel=info &
# 启动Celery Beat（可选）
celery -A tasks beat --loglevel=info &