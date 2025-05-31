# 启动Web服务
uvicorn main:app --reload &
# 启动Celery Worker
# --pool=solo(单线程执行)
# --pool=prefork --concurrency=4 (多进程(prefork) --CPU密集型任务（如算法计算、文件压缩）
# --pool=gevent --concurrency=500  协程模式（eventlet/gevent) ：I/O密集型任务（如API请求、数据库操作）
celery -A tasks.deploy_tasks worker --loglevel=info --pool=solo &
# 启动Celery Beat（可选）
celery -A tasks.deploy_tasks beat --loglevel=info &

# 启动Celery Flower 监控
# http://localhost:5555
celery -A tasks.deploy_tasks flower --port=5555



# docker compose快速启动 https://wenku.csdn.net/doc/7wfh0p9p72