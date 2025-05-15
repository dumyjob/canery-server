function JenkinsStyleLogViewer({ taskId }) {
  const [logs, setLogs] = useState([]);

  useEffect(() => {
    const socket = new SockJS('/ws-logs');
    const client = Stomp.over(socket);

    client.connect({}, () => {
      client.subscribe(`/topic/logs/${taskId}`, message => {
        const log = JSON.parse(message.body);
        setLogs(prev => [...prev,
          <div className={`log-line ${log.type}`}>
            <span className="timestamp">[{log.timestamp}]</span>
            <pre>{log.content}</pre>
          </div>
        ]);
      });
    });

    return () => client.disconnect();
  }, [taskId]);

  return (
    <div className="jenkins-log-container">
      <div className="log-header">
        <Tag color="blue">构建编号: {taskId}</Tag>
        <Progress percent={calculateProgress(logs)} />
      </div>
      <div className="log-body">
        {logs}
      </div>
    </div>
  );
}