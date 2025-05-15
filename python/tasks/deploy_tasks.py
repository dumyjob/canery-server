# tasks/deploy_tasks.py
import os
import requests
import subprocess
import shutil
from celery import Celery
from celery import chain

app = Celery('tasks', broker='redis://redis:6379/0')


@app.task(bind=True, max_retries=3)
def async_deploy_task(self, task_id, git_repo, branch, maven_profile):
    try:
        # 任务链：Git Checkout → Maven Build → Deploy
        workflow = chain(
            git_checkout.s(task_id, git_repo, branch),
            maven_build.s(task_id, maven_profile),
            deploy_to_k8s.s(task_id)
        )
        workflow.apply_async()
    except Exception as e:
        self.retry(exc=e, countdown=60)


@app.task
def git_checkout(task_id, repo_url, branch):
    update_status(task_id, "CHECKING_OUT")
    # 创建临时目录
    work_dir = f"/tmp/{task_id}"
    try:
        os.makedirs(work_dir, exist_ok=True)
        # 执行 Git 命令
        subprocess.run(
            ["git", "clone", "--branch", branch, repo_url, work_dir],
            check=True
        )
        return work_dir
    except subprocess.CalledProcessError | Exception as e:
        update_status(task_id, "FAILED", logs=str(e))
        # 清理临时目录
        shutil.rmtree(work_dir, ignore_errors=True)
        raise



@app.task
def maven_build(work_dir, task_id, profile):
    update_status(task_id, "BUILDING")
    try:
        # 进入代码目录执行 Maven
        subprocess.run(
            ["mvn", "clean", "package", "-P", profile, "-DskipTests"],
            cwd=work_dir,
            check=True
        )
        # 返回构建产物路径
        return f"{work_dir}/target/app.jar"
    except subprocess.CalledProcessError as e:
        update_status(task_id, "FAILED", logs=str(e))
        raise

@app.task
def deploy_to_k8s(jar_path, task_id):
    update_status(task_id, "DEPLOYING")
    try:
        # 构建 Docker 镜像
        subprocess.run(
            ["docker", "build", "-t", f"myapp:{task_id}", "--build-arg", f"JAR_FILE={jar_path}", "."],
            cwd=os.path.dirname(jar_path),
            check=True
        )
        # 推送到镜像仓库
        subprocess.run(
            ["docker", "push", f"myregistry.com/myapp:{task_id}"],
            check=True
        )
        # 更新 Kubernetes 部署
        subprocess.run(
            ["kubectl", "set", "image", "deployment/myapp", f"myapp=myregistry.com/myapp:{task_id}"],
            check=True
        )
        update_status(task_id, "SUCCESS")
    except subprocess.CalledProcessError as e:
        update_status(task_id, "FAILED", logs=str(e))
        raise


def update_status(task_id, status, logs=None):
    # 调用 Java 服务的状态更新 API
    requests.patch(
        f"http://java-service/api/tasks/{task_id}/status",
        json={"status": status, "logs": logs}
    )




#
# @app.task(bind=True, max_retries=3)
# def deploy_ros_task(self, project_id, config):
#     try:
#         # 调用阿里云ROS API
#         ros_client = ROSClient(config['access_key'], config['secret_key'])
#         stack_id = ros_client.create_stack(config['template'])
#
#         # 更新任务状态到数据库（通过Java API）
#         requests.patch(
#             f"http://java-core/api/deployments/{self.request.id}",
#             json={"status": "DEPLOYING", "externalId": stack_id}
#         )
#
#         # 轮询状态直到完成
#         while True:
#             status = ros_client.get_stack_status(stack_id)
#             if status in ["CREATE_COMPLETE", "FAILED"]:
#                 break
#             time.sleep(10)
#
#         # 更新最终状态
#         requests.patch(
#             f"http://java-core/api/deployments/{self.request.id}",
#             json={"status": status}
#         )
#         return stack_id
#     except Exception as e:
#         self.retry(exc=e, countdown=60)
