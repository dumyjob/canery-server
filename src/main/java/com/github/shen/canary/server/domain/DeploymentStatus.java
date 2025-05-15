package com.github.shen.canary.server.domain;

public enum DeploymentStatus {
    /**
     * 任务已创建，等待处理（初始状态）
     * 对应Celery任务状态：PENDING
     */
    PENDING,

    /**
     * 部署进行中（资源创建中）
     * 对应阿里云ROS状态：CREATE_IN_PROGRESS
     * 对应Celery任务状态：STARTED
     */
    DEPLOYING,

    /**
     * 部署成功（资源创建完成且健康检查通过）
     * 对应阿里云ROS状态：CREATE_COMPLETE
     * 对应Celery任务状态：SUCCESS
     */
    SUCCESS,

    /**
     * 部署失败（资源创建失败或超时）
     * 对应阿里云ROS状态：CREATE_FAILED
     * 对应Celery任务状态：FAILURE
     */
    FAILED,

    /**
     * 任务被用户主动取消
     * 对应阿里云ROS状态：ROLLBACK_IN_PROGRESS
     */
    CANCELLED,

    /**
     * 回滚中（资源删除中）
     * 对应阿里云ROS状态：DELETE_IN_PROGRESS
     */
    ROLLBACKING,

    /**
     * 回滚完成（资源已清理）
     * 对应阿里云ROS状态：DELETE_COMPLETE
     */
    ROLLBACKED,

    /**
     * 未知状态（系统异常或状态同步失败）
     */
    UNKNOWN;

    /**
     * 是否为终态（不可再变更）
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == ROLLBACKED;
    }

    /**
     * 将阿里云ROS状态映射为系统状态
     */
    public static DeploymentStatus fromRosStatus(String rosStatus) {
        return switch (rosStatus) {
            case "CREATE_IN_PROGRESS" -> DEPLOYING;
            case "CREATE_COMPLETE" -> SUCCESS;
            case "CREATE_FAILED" -> FAILED;
            case "DELETE_IN_PROGRESS" -> ROLLBACKING;
            case "DELETE_COMPLETE" -> ROLLBACKED;
            default -> UNKNOWN;
        };
    }

    /**
     * 将Celery任务状态映射为系统状态
     */
    public static DeploymentStatus fromCeleryStatus(String celeryStatus) {
        return switch (celeryStatus) {
            case "PENDING" -> PENDING;
            case "STARTED" -> DEPLOYING;
            case "SUCCESS" -> SUCCESS;
            case "FAILURE" -> FAILED;
            default -> UNKNOWN;
        };
    }
}