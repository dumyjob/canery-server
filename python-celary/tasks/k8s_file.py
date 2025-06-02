from jinja2 import Environment, FileSystemLoader


def _get_deployment(config):
    """获取Deployment YAML"""
    # 初始化Jinja2环境
    env = Environment(loader=FileSystemLoader('.'))  # 模板文件在当前目录
    deployment_template = env.get_template("deployment.j2")
    deployment_yaml = deployment_template.render(**config)

    return deployment_yaml


def _get_service(config):
    """获取Service YAML"""
    # 初始化Jinja2环境
    env = Environment(loader=FileSystemLoader('.'))  # 模板文件在当前目录
    service_template = env.get_template("service.j2")
    service_yaml = service_template.render(**config)

    return service_yaml
