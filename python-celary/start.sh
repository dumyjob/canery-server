# 启动Web服务
gunicorn app:app -w 4 -b 0.0.0.0:5000 &
# 启动Celery Worker
celery -A deploy_tasks worker --loglevel=info &
# 启动Celery Beat（可选）
celery -A tasks beat --loglevel=info &



# docker compose快速启动 https://wenku.csdn.net/doc/7wfh0p9p72