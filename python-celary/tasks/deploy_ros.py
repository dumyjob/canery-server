
import os
import time

from celery import Celery

from aliyun.ros import ROSClient
from tasks.deploy_tasks import update_status

app = Celery('tasks', broker='redis://redis:6379/0')

@app.task(bind=True)
def deploy_ros(self, jar_path, task_id):
    # 通过AccessKey初始化客户端（网页6）
    AK = os.getenv('ALIYUN_ACCESS_KEY')  # 建议从环境变量读取
    SECRET = os.getenv('ALIYUN_SECRET_KEY')
    REGION = 'cn-hangzhou'  # 根据业务选择地域，如cn-beijing

    try:
        # 动态生成模板
        template = generate_ros_template(task_id)

        # 调用ROS API（参考网页6）
        ros_client = ROSClient(access_key=AK, secret_key=SECRET, region=REGION)
        # ?JSON.dumps(template)
        stack_id = ros_client.create_stack(template)

        # 状态轮询（网页1提到的自动化管理）
        while True:
            stack_status = ros_client.get_stack_status(stack_id)
            # stack_status -> ??
            update_status(task_id, "SUCCESS", progress=100)

            if stack_status in ["CREATE_COMPLETE", "ROLLBACK_COMPLETE"]:
                break
            time.sleep(10)
    except Exception as e:
        self.retry(exc=e, countdown=60)


def generate_ros_template(task_id):
    return {
        "ROSTemplateFormatVersion": "2015-09-01",
        "Resources": {
            "ECSInstance": {
                "Type": "ALIYUN::ECS::Instance",
                "Properties": {
                    "ImageId": "centos_7_9_x64_20G_alibase_20220419.vhd",
                    "InstanceType": "ecs.c6.large",
                    "UserData": f"docker run -d -p 8080:8080 myregistry.com/myapp:{task_id}"
                }
            }
        }
    }