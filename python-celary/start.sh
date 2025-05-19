# 启动Web服务
uvicorn main:app --reload &
# 启动Celery Worker
celery -A tasks.deploy_tasks worker --loglevel=info --pool=solo &
# 启动Celery Beat（可选）
celery -A tasks.deploy_tasks beat --loglevel=info &

# 启动Celery Flower 监控
# http://localhost:5555
celery -A tasks.deploy_tasks flower --port=5555



# docker compose快速启动 https://wenku.csdn.net/doc/7wfh0p9p72