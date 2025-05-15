# git_utils.py
import os
import shutil
import subprocess
from typing import Optional

def git_checkout(
        repo_url: str,
        target_dir: str,
        branch: str = "main",
        sparse_paths: Optional[list] = None,
        credential: Optional[dict] = None
) -> str:
    """
    执行Git代码检出，支持分支切换和稀疏检出
    :param repo_url: Git仓库地址（支持HTTPS/SSH）
    :param target_dir: 本地存储路径
    :param branch: 目标分支（默认main）
    :param sparse_paths: 稀疏检出路径列表（None时检出全量）
    :param credential: 认证信息 {'username': 'xxx', 'token': 'xxx'}
    :return: 检出后的代码根目录路径
    """
    try:
        # 清理已有目录
        if os.path.exists(target_dir):
            shutil.rmtree(target_dir)
        os.makedirs(target_dir, exist_ok=True)

        # 处理认证信息（网页2/网页4）
        if credential and repo_url.startswith("https://"):
            auth_url = repo_url.replace(
                "https://",
                f"https://{credential['username']}:{credential['token']}@"
            )
        else:
            auth_url = repo_url

        # 初始化仓库并配置稀疏检出（网页1/网页5）
        subprocess.run(
            ["git", "init", target_dir],
            check=True,
            capture_output=True
        )
        os.chdir(target_dir)

        # 配置稀疏检出
        if sparse_paths:
            subprocess.run(["git", "config", "core.sparseCheckout", "true"], check=True)
            with open(".git/info/sparse-checkout", "w") as f:
                f.write("\n".join(sparse_paths))

        # 添加远程仓库并拉取代码（网页3）
        subprocess.run(
            ["git", "remote", "add", "origin", auth_url],
            check=True,
            capture_output=True
        )
        subprocess.run(
            ["git", "pull", "origin", branch, "--depth=1"],
            check=True,
            capture_output=True
        )
        return target_dir

    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"Git检出失败: {e.stderr.decode()}") from e