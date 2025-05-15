# api/main.py
from fastapi import FastAPI
from tasks.status_service import get_task_status

app = FastAPI()

@app.get("/tasks/{task_id}/status")
async def query_task_status(task_id: str):
    status = get_task_status(task_id)
    return {"status": status}