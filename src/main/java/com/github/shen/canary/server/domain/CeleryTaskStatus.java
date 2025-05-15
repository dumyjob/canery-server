package com.github.shen.canary.server.domain;

public enum CeleryTaskStatus {
    /**
     * 任务已创建，但尚未被Worker接收
     */
    PENDING,

    /**
     * 任务已被Worker接收，正在执行中
     */
    STARTED,

    /**
     * 任务成功完成
     */
    SUCCESS,

    /**
     * 任务执行失败
     */
    FAILURE,

    /**
     * 任务因重试策略被重新调度
     */
    RETRY,

    /**
     * 任务被主动撤销
     */
    REVOKED,

    /**
     * 未知状态（Celery返回未识别的状态码）
     */
    UNKNOWN;

    /**
     * 是否为终态（任务不再变更）
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILURE || this == REVOKED;
    }

    /**
     * 将Celery原始状态字符串映射为枚举
     * @param status Celery返回的状态字符串（如"SUCCESS"）
     */
    public static CeleryTaskStatus fromString(String status) {
        if (status == null) {
            return UNKNOWN;
        }
        try {
            return CeleryTaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /**
     * 转换为系统内部部署状态
     */
    public DeploymentStatus toDeploymentStatus() {
        return switch (this) {
            case PENDING -> DeploymentStatus.PENDING;
            case STARTED, RETRY -> DeploymentStatus.DEPLOYING;
            case SUCCESS -> DeploymentStatus.SUCCESS;
            case FAILURE, REVOKED -> DeploymentStatus.FAILED;
            default -> DeploymentStatus.UNKNOWN;
        };
    }
}