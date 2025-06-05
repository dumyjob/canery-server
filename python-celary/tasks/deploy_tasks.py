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
from tasks.k8s_file import _get_deployment, _get_service, _get_ingress

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

DOCKER_REGISTRY = os.environ.get('DOCKER_REGISTRY', '127.0.0.1:5000')

# 默认队列celery; 通过字典解包增强灵活性，推荐用于复杂业务
@app.task(bind=True, max_retries=3)
def deploy_task(self, task_id, config):
    logging.info("run task:" + task_id + ", config:" + str(config))
    try:
        git_repo = config.get("git_repos")
        branch = config.get("branch", "main")
        maven_profile = config.get('maven_profile', 'dev')
        project_type = config.get('project_type', 'java')

        # 任务链：Git Checkout → Maven Build → Deploy
        if project_type == 'spring-boot':
            workflow = chain(
                git_checkout.s(task_id, git_repo, branch),
                maven_build.s(task_id, maven_profile),
                group(deploy_to_k8s.s(task_id, config))
            )
        else:
            workflow = chain(
                git_checkout.s(task_id, git_repo, branch),
                group(deploy_to_k8s.s(task_id, config))
            )
        workflow.apply_async()
    except Exception as e:
        self.retry(exc=e, countdown=60)


@app.task
def git_checkout(task_id, repo_url: str, branch):
    update_status(task_id, "CHECKING_OUT")
    config = git_config.get_auth_config(repo_url)
    sys.stdout.write("auth_url:" + config['auth_url'])
    # 创建临时目录 (windows: D:/canary_home/ linux:
    # /canary_home/tmp/{task_id} windows直接创建可能没权限
    work_dir = Path.home() / "canary_home" / "tmp" / str(task_id)
    try:
        push_cmd(task_id, f'mkdir {work_dir}')
        work_dir.mkdir(parents=True, exist_ok=True)
        if work_dir.exists() and work_dir.is_dir():
            print(f"目录 {work_dir} 创建成功！")
        else:
            print(f"目录创建异常，请检查路径：{work_dir}")
        work_dir = str(work_dir)

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
            text=True,
            encoding='utf-8'
        ).strip()

        push_commit(task_id, commit_id, commit_message)

        return work_dir
    except subprocess.CalledProcessError as e:
        print(f"\033[31m[ERROR] {str(e)}\033[0m", file=sys.stderr)
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
            ["mvn", "-P", profile, "clean", "package", "-DskipTests"],
            cwd=work_dir, task_id=task_id,
            step_identifier={
                "Downloading": 45,
                "Compiling": 60,
                "Testing": 75,
                "BUILD SUCCESS": 95
            }
        )

        # 返回构建产物路径
        return _get_build(task_id, work_dir)
    except subprocess.CalledProcessError as e:
        # 错误信息增强（匹配图片中的unexpected token错误）
        # error_msg = f"构建失败: {e.stderr}"
        # if "unexpected token" in error_msg:
        #     error_msg += "\n可能原因：请检查src/main/java/com/example/UserService.java第42行语法"
        #
        # update_status(task_id, "FAILED", logs=error_msg, progress=40)

        update_status(task_id, "FAILED", logs=str(e), progress=40)
        raise


def _get_build(task_id, work_dir):
    target_dir = Path(work_dir) / "target"
    jar_files = list(target_dir.glob("*.jar"))

    if not jar_files:
        push_logs(task_id, f'在 {target_dir} 中未找到JAR文件')
        raise FileNotFoundError(f"在 {target_dir} 中未找到JAR文件")
    elif len(jar_files) > 1:
        # 选择最新修改的JAR（通常为最近打包结果）
        latest_jar = max(jar_files, key=lambda f: f.stat().st_mtime)
        return str(latest_jar)
    else:
        return str(jar_files[0])



@app.task
def deploy_to_k8s(jar_path, task_id, config):
    update_status(task_id, "DEPLOYING")
    project_name = config.get("project_name")
    project_type = config.get("project_type")

    # 复制 Dockerfile 到构建目录
    shutil.copy(
        _get_dockerfile(),
        Path(jar_path).parent / f"{project_type}.dockerfile"
    )

    # Dockerfile中的copy操作是基于相对路径操作的
    jar_file = Path(jar_path).name

    image_name = f"{project_name}"
    image_tag = f"v{task_id}"
    try:
        # 分阶段执行部署命令
        commands = [
            # 构建 Docker 镜像
            {
                "cmd": ["docker", "build", "-t", f"{image_name}:{image_tag}",
                        "--build-arg", f"JAR_FILE={jar_file}",
                        "-f", f"./{project_type}.dockerfile", "."],
                "progress": 65,
                "error_hint": "镜像构建失败，请检查Dockerfile第5行"
            },
            # 本地镜像关联到私有镜像库
            {
                "cmd": ["docker", "tag", f'{image_name}:{image_tag}', f"{DOCKER_REGISTRY}/{image_name}:{image_tag}"],
                "progress": 80,
                "error_hint": "镜像推送失败，请检查仓库权限"
            },
            # 推送到镜像仓库
            {
                "cmd": ["docker", "push", f"{DOCKER_REGISTRY}/{image_name}:{image_tag}"],
                "progress": 90,
                "error_hint": "镜像推送失败，请检查仓库权限"
            }
        ]

        for step in commands:
            _stream_command(
                step["cmd"],
                cwd=os.path.dirname(jar_path),
                task_id=task_id,
                step_identifier={"Success": step["progress"]}
            )

        deploy_config = {
            "app_name": f"{project_name}",
            "replicas": config.get("pods", 1),
            "image_name": f"{DOCKER_REGISTRY}/{image_name}",
            "image_tag": f"{image_tag}",
            "container_port": config.get("port", "8080"),
            # cpu & memory 单位为 m
            "cpu_limit": config.get("cpu_limit", "1000m"),
            "memory_limit": f'{config.get("memory_limit", "1024")}Mi',
            "cpu": f'{config.get("cpu", "500")}m',
            "memory": config.get("memory", "512Mi"),
            "service_type": "NodePort",
            "service_port": 80,
            "env_vars": {"ENV": "production", "LOG_LEVEL": "info"},

            # 以下参数可选
            "namespace": "default",
            "domain": f"{config.get('domain', f'{project_name}.example.com')}",
        }

        push_logs(task_id, f"正在部署到 Kubernetes，配置如下：\n{deploy_config}")

        # 部署到 Kubernetes
        deploy_k8s(task_id, deploy_config)

        update_status(task_id, "SUCCESS", progress=100)

    except subprocess.CalledProcessError as e:
        # 获取当前步骤的错误提示
        current_step = next((s for s in commands if s["cmd"] == e.cmd), None)
        error_msg = f"部署失败: {e.stderr}"
        if current_step:
            error_msg += f"\n{current_step['error_hint']}"

        update_status(task_id, "FAILED", logs=error_msg, progress=80)
        raise


def _get_dockerfile():
    # 获取脚本所在目录
    script_dir = Path(__file__).resolve().parent
    # 构建目标文件路径
    dockerfile_path = script_dir / "spring-boot.dockerfile"
    # 验证文件存在性
    if dockerfile_path.is_file():
        print(f"找到文件: {dockerfile_path}")
        return dockerfile_path
    else:
        print(f"错误: 同级目录未找到 spring-boot.dockerfile")
        raise FileNotFoundError(f"在 {script_dir} 中未找到spring-boot.dockerfile文件")


def deploy_k8s(task_id, config):
    update_status(task_id, "DEPLOYED")
    logging.info("deploy task:" + task_id + ", config:" + str(config))

    _stream_command(
        ["kubectl", "apply", "-f", "-"],
        cwd=None, task_id=task_id,
        step_identifier={"Success": 10},
        input=_get_deployment(config).encode()
    )

    _stream_command(
        ["kubectl", "apply", "-f", "-"],
        cwd=None, task_id=task_id,
        step_identifier={"Success": 70},
        input=_get_service(config).encode()
    )

    project_type = config.get("project_type")
    if project_type == "react":
        _stream_command(
            ["kubectl", "apply", "-f", "-"],
            cwd=None, task_id=task_id,
            step_identifier={"Success": 90},
            input=_get_ingress(config).encode()
        )


# 新增实时日志捕获函数
@app.task
def _stream_command(cmd, cwd, task_id, step_identifier=None, input=None):
    logging.info("task_id: " + task_id + ",run command:" + str(cmd))
    sys.stdout.write("task_id: " + task_id + ",run command:" + str(cmd) + '\n')
    """实时捕获子进程输出并推送日志"""
    push_cmd(task_id, format_cmd(cmd))

    if platform.system() == "Windows":
        shell_cmd = ["cmd", "/c"] + cmd
    else:
        shell_cmd = ["/bin/sh", "-c"] + [" ".join(cmd)]

    process = subprocess.Popen(
        shell_cmd,
        cwd=cwd,
        stdin=subprocess.PIPE if input else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,  # 合并错误输出到标准输出[1,9](@ref)
        text=True,  # 以文本模式处理输出[8](@ref)
        bufsize=1,  # 行缓冲模式[5,8](@ref)
        universal_newlines=True
    )

    # 写入输入数据（如果有）
    if input:
        process.stdin.write(input.decode() if isinstance(input, bytes) else input)
        process.stdin.close()

    sys.stdout.write(str(cmd) + '\n')
    # 实时输出捕获逻辑
    while True:
        line = process.stdout.readline()
        if not line:
            if process.poll() is not None:  # 进程结束则退出循环[9](@ref)
                break
            continue

        push_logs(task_id, line)
        sys.stdout.write(line)  # 实时打印到控制台[5](@ref)
        sys.stdout.flush()  # 强制刷新缓冲区[9](@ref)

    # 获取最终执行状态
    exit_code = process.poll()
    # 推送最终状态
    status_msg = f"命令执行完成，退出码: {exit_code}"
    push_logs(task_id, status_msg)
    if exit_code != 0:
        raise subprocess.CalledProcessError(
            exit_code, cmd,
            output=line  # 包含最后错误信息的行[1](@ref)
        )
    return exit_code


def format_cmd(cmd_list):
    """转换Maven命令列表为有效字符串"""
    filtered_list = []
    skip_next = False  # 标记是否跳过下一个元素（用于处理-P后的None）

    for i, item in enumerate(cmd_list):
        if skip_next:
            skip_next = False
            continue
        if item == '-P' and i + 1 < len(cmd_list) and cmd_list[i + 1] is None:
            skip_next = True  # 跳过-P和紧随的None
            continue
        if item is not None:
            filtered_list.append(item)

    return " ".join(filtered_list)


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


def push_logs(task_id, line):
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

def push_cmd(task_id, cmd, logs=None, progress=None):
    # 调用 Java 服务的状态更新 API
    try:
        requests.post(
            f"http://localhost:8080/api/tasks/{task_id}/log",
            json={
                "timestamp": datetime.now().isoformat(),
                "content": f"开始执行命令: {str(cmd)}",
                "type": "stdout"
            }
        )
    except (MaxRetryError, NewConnectionError) as e:
        # 记录错误日志（含堆栈跟踪）
        error_msg = f"请求失败: {e}\n{traceback.format_exc()}"
        logging.error(error_msg)
        # 可选：控制台输出简化信息
        print(f"错误: {e}（详见日志文件app.log）")

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
    # target_dir = Path("/canary_home")
    # 检查并创建目录（如果不存在）
    # target_dir.mkdir(parents=True, exist_ok=True)  # 自动创建父目录

    # 验证是否为有效目录
    # if not target_dir.is_dir():
    #     raise NotADirectoryError(f"无效目录: {target_dir}")

    # result = _stream_command(
    #     ["git", "clone", "--branch", 'master', 'https://github.com/dumyjob/canery-ui.git', './tmp/227'],
    #     cwd=target_dir, task_id="1"
    # )
    # print(f"调试输出: {result}")

    format_cmd(['mvn', 'clean', 'package', '-P', None, '-DskipTests'])




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
