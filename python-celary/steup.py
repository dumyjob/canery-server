from setuptools import setup, find_packages

setup(
    name="python-celery",
    version="0.1",
    packages=find_packages(),  # 自动发现 tasks 目录下的包
    package_dir={"": "src"},  # 如果代码在 src 目录下，需指定路径
    install_requires=[
        "fastapi>=0.68.0",
        "celery>=5.2.7",
        "redis>=4.3.4",
    ],
    python_requires=">=3.8",
)
