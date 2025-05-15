# canery-server

# 系统架构流转说明
```mermaid
graph TD
    A[用户请求] --> B[API网关]
    B --> C[认证鉴权]
    C --> D[任务调度]
    D --> E[调用阿里云API<br/>ROS/ACK/云效]
    
    F[数据库<br/>MySQL/MongoDB] --> D
    D --> F
    G[消息队列<br/>RabbitMQ/Redis] --> D
    D --> G

    style A fill:#90EE90,stroke:#333
    style B fill:#87CEEB,stroke:#333
    style C fill:#FFB6C1,stroke:#333
    style D fill:#DAA520,stroke:#333
    style E fill:#FFA07A,stroke:#333
    style F fill:#EEE8AA,stroke:#333
    style G fill:#E6E6FA,stroke:#333
