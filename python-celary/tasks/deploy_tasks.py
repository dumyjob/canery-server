# tasks/deploy_tasks.py
import logging
import os
import platform
import shutil
import subprocess
import sys
import traceback
from datetime import datetime
from pathlib import Path

import requests
from celery import Celery
from celery import chain
from celery import group
from urllib3.exceptions import MaxRetryError, NewConnectionError
from git.git_configs import GitConfig

# from deploy_ros import  deploy_ros

app = Celery('tasks', broker='redis://localhost:6379/0',
             task_serializer='json',  # 任务参数序列化为 json
             result_serializer='json',  # 结果序列化为 JSON
             accept_content=['json'])  # 仅接受这两种格式)

git_config = GitConfig()

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    filename='app.log'
)
logging.info("系统启动完成")  # 示例输出：2025-05-19 14:20:35 - INFO - 系统启动完成 [2,6](@ref)



# 默认队列celery; 通过字典解包增强灵活性，推荐用于复杂业务
@app.task(bind=True, max_retries=3)
def deploy_task(self, task_id, config):
    logging.info("run task:" + task_id + ", config:" + str(config))
    try:
        git_repo = config.get("git_repos")
        branch = config.get("branch", "main")
        maven_profile = config.get('maven_profile')

        # 任务链：Git Checkout → Maven Build → Deploy
        workflow = chain(
            git_checkout.s(task_id, git_repo, branch),
            maven_build.s(task_id, maven_profile),
            group(deploy_to_k8s.s(task_id))
        )


        workflow.apply_async()
    except Exception as e:
        self.retry(exc=e, countdown=60)


@app.task
def git_checkout(task_id, repo_url: str, branch):
    update_status(task_id, "CHECKING_OUT")
    config = git_config.get_auth_config(repo_url)
    sys.stdout.write("auth_url" + config['auth_url'])
    # 创建临时目录
    work_dir = f"canary_home/tmp/{task_id}"
    try:
        os.makedirs(work_dir, exist_ok=True)
        # 执行 Git 命令
        _stream_command(
            ["git", "clone", "--branch", branch, config['auth_url'], work_dir],
            cwd=None,task_id=task_id
        )

        # 获取最后一次commit ID [1,4](@ref)
        commit_id = subprocess.check_output(
            ["git", "rev-parse", "HEAD"],
            cwd=work_dir,
            text=True
        ).strip()

        # 获取commit message（仅提交说明）[1,4](@ref)
        commit_message = subprocess.check_output(
            ["git", "log", "-1", "--pretty=format:%s"],
            cwd=work_dir,
            text=True
        ).strip()

        push_commit(task_id, commit_id, commit_message)

        return work_dir
    except subprocess.CalledProcessError as e:
        print(f"\033[31m[ERROR] {str(e)}\033[0m", file=sys.stderr)
        update_status(task_id, "FAILED", logs=str(e), progress=20)
        # 清理临时目录
        shutil.rmtree(work_dir, ignore_errors=True)
        raise


def push_commit(task_id, commit_id, commit_message):
    try:
        requests.put(
            f"http://localhost:8080/api/tasks/{task_id}/commit-id",
            json={"commitId": commit_id, "message": commit_message}
        )
    except (MaxRetryError, NewConnectionError) as e:
        # 记录错误日志（含堆栈跟踪）
        error_msg = f"请求失败: {e}\n{traceback.format_exc()}"
        logging.error(error_msg)
        # 可选：控制台输出简化信息
        print(f"错误: {e}（详见日志文件app.log）")


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
@app.task
def _stream_command(cmd, cwd, task_id,step_identifier=None):
    logging.info("task_id: " + task_id + ",run command:" + str(cmd))
    sys.stdout.write("task_id: " + task_id + ",run command:" + str(cmd) + '\n')
    """实时捕获子进程输出并推送日志"""

    requests.post(
        f"http://localhost:8080/api/tasks/{task_id}/log",
        json={
            "timestamp": datetime.now().isoformat(),
            "content": f"开始执行命令: {str(cmd)}",
            "type": "stdout"
        }
    )
    if platform.system() == "Windows":
        shell_cmd = ["cmd", "/c"] + cmd
    else:
        shell_cmd = ["/bin/sh", "-c"] + [" ".join(cmd)]

    process = subprocess.Popen(
        shell_cmd,
        cwd=cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,  # 合并错误输出到标准输出[1,9](@ref)
        text=True,  # 以文本模式处理输出[8](@ref)
        bufsize=1,  # 行缓冲模式[5,8](@ref)
        universal_newlines=True
    )

    sys.stdout.write(str(cmd) + '\n')
    # 实时输出捕获逻辑
    while True:
        line = process.stdout.readline()
        if not line:
            if process.poll() is not None:  # 进程结束则退出循环[9](@ref)
                break
            continue

        try:
            requests.post(
                f"http://localhost:8080/api/tasks/{task_id}/log",
                json={
                    "timestamp": datetime.now().isoformat(),
                    "content": line.strip(),  # 去除首尾空白
                    "type": "stdout"
                }
            )
        except Exception as e:
            logging.error(f"日志推送失败: {str(e)}")
            # 失败时回退到本地日志记录
            logging.info(line.strip())
        sys.stdout.write(line)  # 实时打印到控制台[5](@ref)
        sys.stdout.flush()  # 强制刷新缓冲区[9](@ref)

    # 获取最终执行状态
    exit_code = process.poll()
    # 推送最终状态
    status_msg = f"命令执行完成，退出码: {exit_code}"
    requests.post(
        f"http://localhost:8080/api/tasks/{task_id}/log",
        json={
            "timestamp": datetime.now().isoformat(),
            "content": status_msg,
            "type": "stdout"
        }
    )
    if exit_code != 0:
        raise subprocess.CalledProcessError(
            exit_code, cmd,
            output=line  # 包含最后错误信息的行[1](@ref)
        )
    return exit_code



def update_status(task_id, status, logs=None, progress=None):
    # 调用 Java 服务的状态更新 API
    try:
        requests.patch(
            f"http://localhost:8080/api/tasks/{task_id}/status",
            json={"status": status, "logs": logs, progress: progress}
        )
    except (MaxRetryError, NewConnectionError) as e:
        # 记录错误日志（含堆栈跟踪）
        error_msg = f"请求失败: {e}\n{traceback.format_exc()}"
        logging.error(error_msg)
        # 可选：控制台输出简化信息
        print(f"错误: {e}（详见日志文件app.log）")


if __name__ == "__main__":
    # 在此调用目标方法并传入测试参数
    target_dir = Path("/canary_home")
    # 检查并创建目录（如果不存在）
    target_dir.mkdir(parents=True, exist_ok=True)  # 自动创建父目录

    # 验证是否为有效目录
    if not target_dir.is_dir():
        raise NotADirectoryError(f"无效目录: {target_dir}")

    result = _stream_command(
        ["git", "clone", "--branch", 'master', 'https://github.com/dumyjob/canery-ui.git', './tmp/227'],
        cwd=target_dir, task_id="1"
    )
    print(f"调试输出: {result}")


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
