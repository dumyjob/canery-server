# tasks/deploy_tasks.py
import os
from datetime import datetime
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
        _stream_command(
            ["git", "clone", "--branch", branch, repo_url, work_dir],
            cwd=None,task_id=task_id
        )
        return work_dir
    except subprocess.CalledProcessError | Exception as e:
        update_status(task_id, "FAILED", logs=str(e), progress=20)
        # 清理临时目录
        shutil.rmtree(work_dir, ignore_errors=True)
        raise



@app.task
def maven_build(work_dir, task_id, profile):
    update_status(task_id, "BUILDING")
    try:
        # 进入代码目录执行 Maven
        _stream_command(
            ["mvn", "clean", "package", "-P", profile, "-DskipTests"],
            cwd=work_dir, task_id=task_id,
            step_identifier={
                "Downloading": 45,
                "Compiling": 60,
                "Testing": 75,
                "BUILD SUCCESS": 95
            }
        )

        jar_path = f"{work_dir}/target/app.jar"
        if not os.path.exists(jar_path):
            raise FileNotFoundError("构建产物未生成")

        # 返回构建产物路径
        return jar_path
    except subprocess.CalledProcessError as e:
        # 错误信息增强（匹配图片中的unexpected token错误）
        # error_msg = f"构建失败: {e.stderr}"
        # if "unexpected token" in error_msg:
        #     error_msg += "\n可能原因：请检查src/main/java/com/example/UserService.java第42行语法"
        #
        # update_status(task_id, "FAILED", logs=error_msg, progress=40)

        update_status(task_id, "FAILED", logs=str(e), progress=40)
        raise

@app.task
def deploy_to_k8s(jar_path, task_id):
    update_status(task_id, "DEPLOYING")
    try:
        # 分阶段执行部署命令
        commands = [
            # 构建 Docker 镜像
            {
                "cmd": ["docker", "build", "-t", f"myapp:{task_id}", "--build-arg", f"JAR_FILE={jar_path}", "."],
                "progress": 85,
                "error_hint": "镜像构建失败，请检查Dockerfile第5行"
            },
            # 推送到镜像仓库
            {
                "cmd": ["docker", "push", f"myregistry.com/myapp:{task_id}"],
                "progress": 90,
                "error_hint": "镜像推送失败，请检查仓库权限"
            },
            # 更新 Kubernetes 部署
            {
                "cmd": ["kubectl", "set", "image", "deployment/myapp", f"myapp=myregistry.com/myapp:{task_id}"],
                "progress": 95,
                "error_hint": "K8s更新失败，请检查deployment配置"
            }
        ]

        for step in commands:
            _stream_command(
                step["cmd"],
                cwd=os.path.dirname(jar_path),
                task_id=task_id,
                step_identifier={"Success": step["progress"]}
            )

        update_status(task_id, "SUCCESS", progress=100)

    except subprocess.CalledProcessError as e:
        # 获取当前步骤的错误提示
        current_step = next((s for s in commands if s["cmd"] == e.cmd), None)
        error_msg = f"部署失败: {e.stderr}"
        if current_step:
            error_msg += f"\n{current_step['error_hint']}"

        update_status(task_id, "FAILED", logs=error_msg, progress=80)
        raise


# 新增实时日志捕获函数
def _stream_command(cmd, cwd, task_id,step_identifier=None):
    """实时捕获子进程输出并推送日志"""
    process = subprocess.Popen(
        cmd,
        cwd=cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    # 逐行读取输出
    while True:
        line = process.stdout.readline()
        if not line and process.poll() is not None:
            break
        if line:
            # 推送实时日志到后端
            requests.post(
                f"http://java-service/api/tasks/{task_id}/log",
                json={
                    "timestamp": datetime.now().isoformat(),
                    "content": line.strip(),
                    "type": "stdout"
                }
            )

            # 进度更新逻辑
            if step_identifier:
                for keyword, progress in step_identifier.items():
                    if keyword in line:
                        requests.patch(
                            f"http://java-service/api/tasks/{task_id}/progress",
                            json={"progress": progress}
                        )

    if process.returncode != 0:
        raise subprocess.CalledProcessError(process.returncode, cmd)



def update_status(task_id, status, logs=None, progress=None):
    # 调用 Java 服务的状态更新 API
    requests.patch(
        f"http://java-service/api/tasks/{task_id}/status",
        json={"status": status, "logs": logs, progress: progress}
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
