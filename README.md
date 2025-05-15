# canery-server

用户请求 → API网关 → 认证鉴权 → 任务调度 → 调用阿里云API（ROS/ACK/云效）
                              ↑
                      数据库（MySQL/MongoDB）
                              ↑
                      消息队列（RabbitMQ/Redis）
