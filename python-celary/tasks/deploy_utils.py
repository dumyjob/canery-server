# deploy_utils.py
from kubernetes import client, config
from kubernetes.client.rest import ApiException
import yaml
import os

def deploy_to_k8s(
        image_name: str,
        namespace: str = "default",
        kubeconfig_path: str = None,
        deployment_template: str = None
) -> dict:
    """
    部署应用到Kubernetes集群
    :param image_name: Docker镜像名称（含tag）
    :param namespace: 目标命名空间（默认default）
    :param kubeconfig_path: kubeconfig文件路径（None时使用默认配置）
    :param deployment_template: 自定义部署模板路径
    :return: 部署结果字典
    """
    try:
        # 加载K8S配置（网页9/网页11）
        if kubeconfig_path:
            config.load_kube_config(config_file=kubeconfig_path)
        else:
            config.load_incluster_config()  # 适用于集群内运行

        # 动态生成部署模板（网页10）
        if not deployment_template:
            template = _default_deployment_template(image_name)
        else:
            with open(deployment_template) as f:
                template = yaml.safe_load(f)

        # 创建API客户端
        api_instance = client.AppsV1Api()

        # 提交部署（网页11）
        resp = api_instance.create_namespaced_deployment(
            namespace=namespace,
            body=template,
            pretty=True
        )
        return {
            "status": "SUCCESS",
            "deployment_name": resp.metadata.name,
            "replicas": resp.spec.replicas
        }

    except ApiException as e:
        return {
            "status": "FAILED",
            "error": f"K8S API异常: {e.reason}",
            "detail": e.body
        }

def _default_deployment_template(image: str) -> dict:
    """生成默认部署模板"""
    return {
        "apiVersion": "apps/v1",
        "kind": "Deployment",
        "metadata": {"name": "auto-deploy", "labels": {"app": "auto-deploy"}},
        "spec": {
            "replicas": 1,
            "selector": {"matchLabels": {"app": "auto-deploy"}},
            "template": {
                "metadata": {"labels": {"app": "auto-deploy"}},
                "spec": {
                    "containers": [{
                        "name": "main",
                        "image": image,
                        "ports": [{"containerPort": 8080}]
                    }]
                }
            }
        }
    }