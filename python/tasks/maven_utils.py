# maven_utils.py
import os
import subprocess
from pathlib import Path

def maven_build(
        project_dir: str,
        build_args: list = None,
        mvn_exec: str = "mvn"
) -> dict:
    """
    执行Maven构建并返回构建产物信息
    :param project_dir: 项目根目录路径
    :param build_args: 自定义构建参数（如["-DskipTests", "-Pprod"]）
    :param mvn_exec: Maven可执行文件路径（默认使用系统PATH）
    :return: 构建产物信息字典（包含JAR路径、版本等）
    """
    try:
        # 组合构建命令（网页7/网页8）
        base_cmd = [mvn_exec, "clean", "package"]
        if build_args:
            base_cmd.extend(build_args)

        # 执行构建
        result = subprocess.run(
            base_cmd,
            cwd=project_dir,
            check=True,
            capture_output=True,
            text=True
        )

        # 定位构建产物（网页6）
        target_dir = os.path.join(project_dir, "target")
        jars = list(Path(target_dir).glob("*.jar"))
        if not jars:
            raise FileNotFoundError("未找到构建产物JAR文件")

        return {
            "success": True,
            "main_jar": str(jars[0].absolute()),
            "version": _parse_maven_version(project_dir),
            "build_log": result.stdout
        }

    except subprocess.CalledProcessError as e:
        return {
            "success": False,
            "error": f"Maven构建失败: {e.stderr}",
            "exit_code": e.returncode
        }

def _parse_maven_version(project_dir: str) -> str:
    """从pom.xml解析项目版本"""
    from xml.etree import ElementTree as ET
    pom_path = os.path.join(project_dir, "pom.xml")
    tree = ET.parse(pom_path)
    root = tree.getroot()
    ns = {'mvn': 'http://maven.apache.org/POM/4.0.0'}
    return root.findtext("mvn:version", namespaces=ns) or "UNKNOWN"