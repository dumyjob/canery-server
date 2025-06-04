from jinja2 import Environment, FileSystemLoader
from pathlib import Path

def _get_deployment(config):
    """获取Deployment YAML"""
    script_dir = Path(__file__).resolve().parent

    # 验证模板文件存在性
    template_path = script_dir / "deployment.j2"
    if not template_path.is_file():
        raise FileNotFoundError(f"在 {script_dir} 中未找到deployment.j2文件")

    env = Environment(loader=FileSystemLoader(script_dir))  # 模板文件在当前目录
    deployment_template = env.get_template("deployment.j2")
    deployment_yaml = deployment_template.render(**config)

    return deployment_yaml


def _get_service(config):
    """获取Service YAML"""
    script_dir = Path(__file__).resolve().parent

    # 验证模板文件存在性
    template_path = script_dir / "service.j2"
    if not template_path.is_file():
        raise FileNotFoundError(f"在 {script_dir} 中未找到service.j2文件")
    env = Environment(loader=FileSystemLoader(script_dir))  # 模板文件在当前目录
    service_template = env.get_template("service.j2")
    service_yaml = service_template.render(**config)

    return service_yaml
