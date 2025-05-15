# tasks/deploy_tasks.py
from celery import Celery
from aliyun.ros import ROSClient
from aliyun.ack import ACKClient

app = Celery('tasks', broker='redis://redis:6379/0')

@app.task(bind=True, max_retries=3)
def deploy_ros_task(self, project_id, config):
    try:
        # 调用阿里云ROS API
        ros_client = ROSClient(config['access_key'], config['secret_key'])
        stack_id = ros_client.create_stack(config['template'])

        # 更新任务状态到数据库（通过Java API）
        requests.patch(
            f"http://java-core/api/deployments/{self.request.id}",
            json={"status": "DEPLOYING", "externalId": stack_id}
        )

        # 轮询状态直到完成
        while True:
            status = ros_client.get_stack_status(stack_id)
            if status in ["CREATE_COMPLETE", "FAILED"]:
                break
            time.sleep(10)

        # 更新最终状态
        requests.patch(
            f"http://java-core/api/deployments/{self.request.id}",
            json={"status": status}
        )
        return stack_id
    except Exception as e:
        self.retry(exc=e, countdown=60)