import configparser
import logging
import os
import re
from urllib.parse import quote

# 日志配置
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("GitConfigManager")


class GitConfig:
    """
    Git服务器配置管理器（基于配置文件的实现）
    """
    # 配置字典缓存
    _config_cache = None
    _cache_timestamp = 0

    def __init__(self, config_file_path: str = None):
        """
        初始化配置管理器

        :param config_file_path: 配置文件路径（可选，默认使用环境变量或标准位置）
        """
        self.config_file_path = config_file_path or self._get_default_config_path()
        self._load_config()

    def _get_default_config_path(self) -> str:
        """
        获取默认配置文件路径

        优先级：
        1. GIT_CONFIG_PATH 环境变量
        2. 当前目录下的 git_config.ini
        3. 用户目录下的 .git_config.ini
        4. /etc/git_config.ini
        """
        if "GIT_CONFIG_PATH" in os.environ:
            return os.environ["GIT_CONFIG_PATH"]

        # 尝试不同位置的配置文件
        search_paths = [
            "git_config.ini",
            os.path.expanduser("~/.git_config.ini"),
            "/etc/git_config.ini"
        ]

        for path in search_paths:
            if os.path.exists(path):
                logger.info(f"Using config file: {path}")
                return path

        raise FileNotFoundError("No valid configuration file found")

    def _load_config(self):
        """
        加载并解析配置文件，支持热重载
        """
        # 检查文件修改时间
        current_time = os.path.getmtime(self.config_file_path)

        # 如果缓存存在且文件未修改，直接返回缓存
        if self._config_cache is not None and current_time <= self._cache_timestamp:
            return

        try:
            logger.info(f"Loading config from: {self.config_file_path}")
            config_parser = configparser.ConfigParser()
            config_parser.read(self.config_file_path, encoding='utf-8')

            # 将配置转换为字典
            git_config = {
                'global': dict(config_parser['GLOBAL']),
                'servers': []
            }

            # 遍历所有服务器配置
            for section in config_parser.sections():
                if section.startswith('SERVER:'):
                    server_name = section.split(':', 1)[1]
                    server_data = dict(config_parser[section])

                    # 处理正则表达式
                    if 'repo_pattern' in server_data:
                        server_data['repo_pattern'] = re.compile(server_data['repo_pattern'])

                    # 处理加密的访问令牌
                    if 'access_token_enc' in server_data:
                        server_data['access_token'] = self._decrypt_token(server_data['access_token_enc'])
                        del server_data['access_token_enc']

                    git_config['servers'].append({
                        'name': server_name,
                        **server_data
                    })

            # 更新缓存
            self._config_cache = git_config
            self._cache_timestamp = current_time
            logger.info("Configuration loaded successfully")

        except Exception as e:
            logger.error(f"Error loading configuration: {str(e)}")
            raise

    def _decrypt_token(self, encrypted_token: str) -> str:
        """
        解密访问令牌

        :param encrypted_token: 加密的令牌字符串
        :return: 解密后的令牌
        """
        try:
            # 简单示例：Base64 "解密" - 生产环境应使用KMS或Vault
            # import base64
            # return base64.b64decode(encrypted_token).decode()

            # 使用环境变量中的密钥解密
            if "ENCRYPTION_KEY" in os.environ:
                # 使用cryptography库进行安全的解密
                from cryptography.fernet import Fernet
                key = os.environ["ENCRYPTION_KEY"].encode()
                return Fernet(key).decrypt(encrypted_token.encode()).decode()
            else:
                # 没有加密密钥时直接返回
                return encrypted_token

        except Exception as e:
            logger.warning(f"Token decryption failed: {str(e)}")
            return encrypted_token

    def get_auth_config(self, repo_url: str) -> dict:
        """
        获取指定仓库URL的认证配置

        :param repo_url: 仓库URL (e.g., "https://github.com/user/repo.git")
        :return: 包含认证信息的字典
        """
        # 确保配置已加载
        self._load_config()

        # 获取全局配置
        global_config = self._config_cache['global']
        auth_method = global_config.get('auth_method', 'https')

        # 查找匹配的服务器配置
        for server in self._config_cache['servers']:
            # 检查是否匹配正则表达式
            if 'repo_pattern' in server and server['repo_pattern'].search(repo_url):
                # 处理用户名和访问令牌
                username = server.get('username', 'git')
                token = server.get('access_token', '')

                # 特殊令牌处理（如GitHub、GitLab的PAT）
                if token and auth_method == 'https':
                    # 确保URL编码安全的令牌
                    safe_token = quote(token, safe="")
                    auth_url = repo_url.replace(
                        "https://",
                        f"https://{username}:{safe_token}@"
                    )
                else:
                    auth_url = repo_url

                return {
                    'name': server['name'],
                    'server_type': server.get('server_type', 'generic'),
                    'username': username,
                    'token': token,
                    'auth_url': auth_url,
                    'base_url': server.get('base_url', ''),
                    'auth_method': auth_method
                }

        # 没有匹配的配置时回退到全局配置
        logger.warning(f"No specific config for {repo_url}, using global config")
        return {
            'server_type': 'generic',
            'username': global_config.get('username', 'git'),
            'token': global_config.get('access_token', ''),
            'auth_url': repo_url,
            'base_url': '',
            'auth_method': auth_method
        }

    @staticmethod
    def generate_sample_config(file_path: str):
        """
        生成示例配置文件

        :param file_path: 配置文件路径
        """
        config = configparser.ConfigParser()

        # 全局配置部分
        config['GLOBAL'] = {
            'username': 'deploy_bot',
            'auth_method': 'https',
            'log_level': 'INFO',
            # 'access_token': 'your_global_token_here',  # 安全起见不包含在示例中
        }

        # GitHub 配置
        config['SERVER:github'] = {
            'server_type': 'github',
            'repo_pattern': r'.*github\.com/.*',
            'username': 'github_deploy',
            # 'access_token_enc': 'b3VyX2VuY3J5cHRlZF90b2tlbg==',  # 加密的令牌示例
        }

        # GitLab 自建实例配置
        config['SERVER:my_gitlab'] = {
            'server_type': 'gitlab',
            'base_url': 'https://gitlab.example.com',
            'repo_pattern': r'.*gitlab\.example\.com/.*',
            'username': 'gitlab_deploy',
            # 'access_token': 'your_gitlab_token_here',
        }

        # 默认配置（当没有匹配时使用）
        config['SERVER:default'] = {
            'server_type': 'generic',
            'repo_pattern': r'.*',
            'username': 'git',
        }

        # 写入文件
        with open(file_path, 'w') as configfile:
            config.write(configfile)
        logger.info(f"Sample configuration file created: {file_path}")


# 使用示例
if __name__ == "__main__":
    # 生成示例配置文件（如果需要）
    # GitConfig.generate_sample_config("git_config.ini")

    # 使用配置管理器
    config_manager = GitConfig()

    # 测试仓库URL
    test_urls = [
        "https://github.com/user/repo.git",
        "https://gitlab.example.com/group/project.git",
        "https://unknown.git.server/repo.git",
        "https://github.com/dumyjob/canery-server.git"
    ]

    for url in test_urls:
        try:
            config = config_manager.get_auth_config(url)
            print(f"\nURL: {url}")
            print(f"Matched Config: {config['name'] if 'name' in config else 'default'}")
            print(f"Auth URL: {config['auth_url']}")
            print(f"Username: {config['username']}")
            print(f"Method: {config['auth_method']}")
        except Exception as e:
            print(f"Error for {url}: {str(e)}")
