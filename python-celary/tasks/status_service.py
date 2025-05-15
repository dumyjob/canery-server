# tasks/status_service.py
from celery.result import AsyncResult
from tasks.celery_config import app

def get_task_status(task_id: str) -> str:
    """查询 Celery 任务状态"""
    try:
        result = AsyncResult(id=task_id, app=app)
        return result.state  # 返回状态字符串，如 "SUCCESS"
    except Exception as e:
        # 处理结果后端不可用或任务ID无效
        print(f"查询任务状态失败: {e}")
        return "UNKNOWN"